package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.InvoiceItemsTable
import com.printbusinesskmp.database.tables.InvoicesTable
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.InvoiceClient
import com.printbusinesskmp.models.InvoiceItem
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class InvoiceRepository {

    suspend fun allInvoices(): List<Invoice> = dbQuery {
        InvoicesTable.selectAll().map { invoiceRow ->
            val invoiceId = invoiceRow[InvoicesTable.id]
            val items = getInvoiceItems(invoiceId)
            toInvoice(invoiceRow, items)
        }
    }

    suspend fun invoiceById(id: String): Invoice? = dbQuery {
        InvoicesTable.selectAll().where { InvoicesTable.id eq id }
            .map { invoiceRow ->
                val items = getInvoiceItems(id)
                toInvoice(invoiceRow, items)
            }
            .singleOrNull()
    }

    suspend fun invoicesByOrderId(orderId: String): List<Invoice> = dbQuery {
        InvoicesTable.selectAll().where { InvoicesTable.orderId eq orderId }
            .map { invoiceRow ->
                val invoiceId = invoiceRow[InvoicesTable.id]
                val items = getInvoiceItems(invoiceId)
                toInvoice(invoiceRow, items)
            }
    }

    suspend fun getNextInvoiceNumber(): Int = dbQuery {
        val maxNumber = InvoicesTable
            .select(InvoicesTable.number)
            .maxOfOrNull { it[InvoicesTable.number] } ?: 0
        maxNumber + 1
    }

    suspend fun addInvoice(invoice: Invoice): Invoice = dbQuery {
        val insertId = invoice.id.ifEmpty { UUID.randomUUID().toString() }

        // Insert invoice
        InvoicesTable.insert {
            it[id] = insertId
            it[number] = invoice.number
            it[date] = java.time.Instant.ofEpochMilli(invoice.date.toEpochMilliseconds())
            it[orderId] = invoice.orderId
            it[clientName] = invoice.client.name
            it[clientPhone] = invoice.client.phone
            it[clientEmail] = invoice.client.email
            it[clientAddress] = invoice.client.address
            it[totalAmount] = invoice.totalAmount
            it[notes] = invoice.notes
            it[generatedAt] = java.time.Instant.ofEpochMilli(invoice.generatedAt.toEpochMilliseconds())
            it[filePath] = invoice.filePath
        }

        // Insert invoice items
        invoice.items.forEach { item ->
            val itemId = UUID.randomUUID().toString()
            InvoiceItemsTable.insert {
                it[id] = itemId
                it[invoiceId] = insertId
                it[itemNumber] = item.number
                it[description] = item.description
                it[quantity] = item.quantity
                it[unit] = item.unit
                it[pricePerUnit] = item.pricePerUnit
                it[totalPrice] = item.totalPrice
            }
        }

        // Query directly within same transaction
        val invoiceRow = InvoicesTable.selectAll().where { InvoicesTable.id eq insertId }.single()
        val items = getInvoiceItems(insertId)
        toInvoice(invoiceRow, items)
    }

    suspend fun updateInvoiceFilePath(id: String, filePath: String): Invoice? = dbQuery {
        InvoicesTable.update({ InvoicesTable.id eq id }) {
            it[InvoicesTable.filePath] = filePath
        }
        invoiceById(id)
    }

    suspend fun deleteInvoice(id: String): Boolean = dbQuery {
        InvoiceItemsTable.deleteWhere { invoiceId eq id }
        InvoicesTable.deleteWhere { InvoicesTable.id eq id } > 0
    }

    private fun getInvoiceItems(invoiceId: String): List<InvoiceItem> {
        return InvoiceItemsTable.selectAll().where { InvoiceItemsTable.invoiceId eq invoiceId }
            .map { toInvoiceItem(it) }
    }

    private fun toInvoice(row: ResultRow, items: List<InvoiceItem>): Invoice =
        Invoice(
            id = row[InvoicesTable.id],
            number = row[InvoicesTable.number],
            date = kotlin.time.Instant.fromEpochMilliseconds(
                row[InvoicesTable.date].toEpochMilli()
            ),
            orderId = row[InvoicesTable.orderId],
            client = InvoiceClient(
                name = row[InvoicesTable.clientName],
                phone = row[InvoicesTable.clientPhone],
                email = row[InvoicesTable.clientEmail],
                address = row[InvoicesTable.clientAddress]
            ),
            items = items,
            totalAmount = row[InvoicesTable.totalAmount],
            notes = row[InvoicesTable.notes],
            generatedAt = kotlin.time.Instant.fromEpochMilliseconds(
                row[InvoicesTable.generatedAt].toEpochMilli()
            ),
            filePath = row[InvoicesTable.filePath]
        )

    private fun toInvoiceItem(row: ResultRow): InvoiceItem =
        InvoiceItem(
            number = row[InvoiceItemsTable.itemNumber],
            description = row[InvoiceItemsTable.description],
            quantity = row[InvoiceItemsTable.quantity],
            unit = row[InvoiceItemsTable.unit],
            pricePerUnit = row[InvoiceItemsTable.pricePerUnit],
            totalPrice = row[InvoiceItemsTable.totalPrice]
        )
}
