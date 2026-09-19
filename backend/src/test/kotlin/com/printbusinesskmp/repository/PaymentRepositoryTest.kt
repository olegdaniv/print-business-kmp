package com.printbusinesskmp.repository

import com.printbusinesskmp.database.PaymentsMigration
import com.printbusinesskmp.database.tables.AppSettingsTable
import com.printbusinesskmp.database.tables.ClientsTable
import com.printbusinesskmp.database.tables.OrderItemsTable
import com.printbusinesskmp.database.tables.OrdersTable
import com.printbusinesskmp.database.tables.PaymentAllocationsTable
import com.printbusinesskmp.database.tables.PaymentsTable
import com.printbusinesskmp.models.PaymentAllocation
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.models.PaymentUpsertRequest
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PaymentRepositoryTest {

    private val payments = PaymentRepository()
    private val orders = OrderRepository()

    @BeforeTest
    fun setUp() {
        // Repositories use the default database; point it at a fresh one per test.
        TransactionManager.defaultDatabase = Database.connect(
            url = "jdbc:h2:mem:payments_${UUID.randomUUID().toString().replace("-", "")};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction {
            SchemaUtils.create(
                ClientsTable, OrdersTable, OrderItemsTable, AppSettingsTable, PaymentsTable, PaymentAllocationsTable
            )
        }
    }

    @AfterTest
    fun tearDown() {
        TransactionManager.defaultDatabase = null
    }

    @Test
    fun `order paid in two parts goes from partial to paid`() = runBlocking {
        val client = insertClient()
        val order = insertOrder(client, total = 10_000.0)

        payments.addPayment(request(client, 5_000.0, PaymentAllocation(order, 5_000.0)))
        orders.orderById(order)!!.let {
            assertEquals(PaymentStatus.PARTIAL, it.paymentStatus)
            assertEquals(5_000.0, it.paidAmount)
            assertEquals(5_000.0, it.balanceDue)
        }

        payments.addPayment(request(client, 5_000.0, PaymentAllocation(order, 5_000.0)))
        assertEquals(PaymentStatus.PAID, orders.orderById(order)!!.paymentStatus)
        assertEquals(2, payments.allPayments(orderId = order).size)
    }

    @Test
    fun `one payment split across orders leaves remainder as advance`() = runBlocking {
        val client = insertClient()
        val first = insertOrder(client, total = 3_000.0)
        val second = insertOrder(client, total = 2_000.0)

        val payment = payments.addPayment(
            request(client, 6_000.0, PaymentAllocation(first, 3_000.0), PaymentAllocation(second, 2_000.0))
        )

        assertEquals(1_000.0, payment.unallocatedAmount)
        val all = orders.allOrders().associateBy { it.id }
        assertEquals(PaymentStatus.PAID, all.getValue(first).paymentStatus)
        assertEquals(PaymentStatus.PAID, all.getValue(second).paymentStatus)
    }

    @Test
    fun `rejects allocating more than received or to another client's order`() = runBlocking {
        val client = insertClient()
        val stranger = insertClient()
        val order = insertOrder(client, total = 1_000.0)
        val strangerOrder = insertOrder(stranger, total = 1_000.0)

        assertFailsWith<IllegalArgumentException> {
            payments.addPayment(request(client, 500.0, PaymentAllocation(order, 600.0)))
        }
        assertFailsWith<IllegalArgumentException> {
            payments.addPayment(request(client, 500.0, PaymentAllocation(strangerOrder, 500.0)))
        }
        Unit
    }

    @Test
    fun `deleting an order keeps the payment as unallocated`() = runBlocking {
        val client = insertClient()
        val order = insertOrder(client, total = 1_000.0)
        val payment = payments.addPayment(request(client, 1_000.0, PaymentAllocation(order, 1_000.0)))

        orders.deleteOrder(order)

        val kept = assertNotNull(payments.paymentById(payment.id))
        assertEquals(1_000.0, kept.unallocatedAmount)
    }

    @Test
    fun `migration turns legacy paid orders into payments once and lists partial ones`() = runBlocking {
        val client = insertClient()
        val paid = insertOrder(client, total = 1_500.0, legacyStatus = PaymentStatus.PAID)
        val partial = insertOrder(client, total = 800.0, legacyStatus = PaymentStatus.PARTIAL)
        insertOrder(client, total = 900.0)

        transaction { PaymentsMigration.migrateLegacyPaidOrders() }
        transaction { PaymentsMigration.migrateLegacyPaidOrders() }

        val migrated = payments.allPayments()
        assertEquals(1, migrated.size)
        assertEquals(PaymentsMigration.MIGRATED_NOTE, migrated.single().notes)
        assertEquals(PaymentStatus.PAID, orders.orderById(paid)!!.paymentStatus)
        assertEquals(PaymentStatus.UNPAID, orders.orderById(partial)!!.paymentStatus)
        assertEquals(listOf(partial), payments.legacyPartialOrderIds())

        payments.addPayment(request(client, 300.0, PaymentAllocation(partial, 300.0)))
        assertEquals(emptyList(), payments.legacyPartialOrderIds())
    }

    private fun request(clientId: String, amount: Double, vararg allocations: PaymentAllocation) =
        PaymentUpsertRequest(
            clientId = clientId,
            paidAtEpochMs = Instant.now().toEpochMilli(),
            amount = amount,
            allocations = allocations.toList()
        )

    private fun insertClient(): String = transaction {
        val id = UUID.randomUUID().toString()
        ClientsTable.insert {
            it[ClientsTable.id] = id
            it[type] = "INDIVIDUAL"
            it[displayName] = "Client $id"
            it[phone] = "+380000000000"
            it[address] = "Kyiv"
        }
        id
    }

    private fun insertOrder(
        clientId: String,
        total: Double,
        legacyStatus: PaymentStatus = PaymentStatus.UNPAID
    ): String = transaction {
        val id = UUID.randomUUID().toString()
        OrdersTable.insert {
            it[OrdersTable.id] = id
            it[OrdersTable.clientId] = clientId
            it[status] = "IN_PRODUCTION"
            it[paymentStatus] = legacyStatus.name
            it[totalCost] = 0.0
            it[totalPrice] = total
            it[profit] = total
        }
        id
    }
}
