package com.printbusinesskmp.repository

import com.printbusinesskmp.database.DatabaseFactory.dbQuery
import com.printbusinesskmp.database.tables.InvoiceLinesTable
import com.printbusinesskmp.database.tables.InvoicesTable
import com.printbusinesskmp.models.ClientType
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.models.InvoiceClientSnapshot
import com.printbusinesskmp.models.InvoiceLine
import com.printbusinesskmp.models.InvoiceSellerSnapshot
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class InvoiceRepository {

    suspend fun allInvoices(): List<Invoice> = dbQuery {
        InvoicesTable.selectAll().map { row ->
            val invoiceId = row[InvoicesTable.id]
            toInvoice(row, getLines(invoiceId))
        }
    }

    suspend fun invoiceById(id: String): Invoice? = dbQuery {
        InvoicesTable.selectAll()
            .where { InvoicesTable.id eq id }
            .map { row -> toInvoice(row, getLines(id)) }
            .singleOrNull()
    }

    suspend fun invoicesByOrderId(orderId: String): List<Invoice> = dbQuery {
        InvoicesTable.selectAll()
            .where { InvoicesTable.orderId eq orderId }
            .map { row ->
                val invoiceId = row[InvoicesTable.id]
                toInvoice(row, getLines(invoiceId))
            }
    }

    suspend fun nextInvoiceNumber(prefix: String, padding: Int): String = dbQuery {
        val maxSeq = InvoicesTable.selectAll()
            .where { InvoicesTable.number like "$prefix%" }
            .mapNotNull { row ->
                row[InvoicesTable.number]
                    .removePrefix(prefix)
                    .toIntOrNull()
            }
            .maxOrNull() ?: 0
        "$prefix${(maxSeq + 1).toString().padStart(padding, '0')}"
    }

    suspend fun numberExists(number: String, excludeId: String? = null): Boolean = dbQuery {
        InvoicesTable.selectAll()
            .where { InvoicesTable.number eq number }
            .any { excludeId == null || it[InvoicesTable.id] != excludeId }
    }

    suspend fun updateInvoiceNumber(id: String, number: String): Invoice? = dbQuery {
        val changed = InvoicesTable.update({ InvoicesTable.id eq id }) {
            it[InvoicesTable.number] = number
        }
        if (changed == 0) {
            null
        } else {
            val row = InvoicesTable.selectAll().where { InvoicesTable.id eq id }.single()
            toInvoice(row, getLines(id))
        }
    }

    suspend fun addInvoice(invoice: Invoice): Invoice = dbQuery {
        val id = invoice.id.ifBlank { UUID.randomUUID().toString() }

        InvoicesTable.insert {
            it[InvoicesTable.id] = id
            it[number] = invoice.number
            it[orderId] = invoice.orderId
            it[clientId] = invoice.clientId
            it[issuedAt] = Instant.ofEpochMilli(invoice.issuedAt.toEpochMilliseconds())
            it[validUntil] = invoice.validUntil?.let { v -> Instant.ofEpochMilli(v.toEpochMilliseconds()) }
            it[payer] = invoice.payer
            it[orderRef] = invoice.orderRef

            it[sellerOwnerName] = invoice.seller.ownerName
            it[sellerTaxId] = invoice.seller.taxId
            it[sellerAddress] = invoice.seller.address
            it[sellerIban] = invoice.seller.iban
            it[sellerBankName] = invoice.seller.bankName
            it[sellerTaxPercent] = invoice.seller.taxPercent
            it[sellerTaxNote] = invoice.seller.taxNote
            it[sellerMfo] = invoice.seller.mfo
            it[sellerIpn] = invoice.seller.ipn

            it[clientType] = invoice.client.type.name
            it[clientName] = invoice.client.name
            it[clientAddress] = invoice.client.address
            it[clientPhone] = invoice.client.phone
            it[clientEmail] = invoice.client.email

            it[subtotal] = invoice.subtotal
            it[discountAmount] = invoice.discountAmount
            it[taxAmount] = invoice.taxAmount
            it[totalAmount] = invoice.totalAmount
            it[notes] = invoice.notes
            it[filePath] = invoice.filePath
        }

        invoice.lines.forEach { line ->
            InvoiceLinesTable.insert {
                it[InvoiceLinesTable.id] = UUID.randomUUID().toString()
                it[InvoiceLinesTable.invoiceId] = id
                it[InvoiceLinesTable.lineNumber] = line.lineNumber
                it[InvoiceLinesTable.description] = line.description
                it[InvoiceLinesTable.quantity] = line.quantity
                it[InvoiceLinesTable.unit] = line.unit
                it[InvoiceLinesTable.usedMeters] = line.usedMeters
                it[InvoiceLinesTable.unitPrice] = line.unitPrice
                it[InvoiceLinesTable.lineTotal] = line.lineTotal
            }
        }

        val row = InvoicesTable.selectAll().where { InvoicesTable.id eq id }.single()
        toInvoice(row, getLines(id))
    }

    suspend fun updateInvoice(id: String, invoice: Invoice): Invoice? = dbQuery {
        val changed = InvoicesTable.update({ InvoicesTable.id eq id }) {
            it[clientId] = invoice.clientId
            it[validUntil] = invoice.validUntil?.let { v -> Instant.ofEpochMilli(v.toEpochMilliseconds()) }
            it[payer] = invoice.payer
            it[orderRef] = invoice.orderRef

            it[clientType] = invoice.client.type.name
            it[clientName] = invoice.client.name
            it[clientAddress] = invoice.client.address
            it[clientPhone] = invoice.client.phone
            it[clientEmail] = invoice.client.email

            it[subtotal] = invoice.subtotal
            it[discountAmount] = invoice.discountAmount
            it[taxAmount] = invoice.taxAmount
            it[totalAmount] = invoice.totalAmount
            it[notes] = invoice.notes
            it[filePath] = invoice.filePath
        }

        if (changed == 0) return@dbQuery null

        InvoiceLinesTable.deleteWhere { invoiceId eq id }
        invoice.lines.forEach { line ->
            InvoiceLinesTable.insert {
                it[InvoiceLinesTable.id] = UUID.randomUUID().toString()
                it[InvoiceLinesTable.invoiceId] = id
                it[InvoiceLinesTable.lineNumber] = line.lineNumber
                it[InvoiceLinesTable.description] = line.description
                it[InvoiceLinesTable.quantity] = line.quantity
                it[InvoiceLinesTable.unit] = line.unit
                it[InvoiceLinesTable.usedMeters] = line.usedMeters
                it[InvoiceLinesTable.unitPrice] = line.unitPrice
                it[InvoiceLinesTable.lineTotal] = line.lineTotal
            }
        }

        val row = InvoicesTable.selectAll().where { InvoicesTable.id eq id }.single()
        toInvoice(row, getLines(id))
    }

    suspend fun updateInvoiceFilePath(id: String, filePath: String): Invoice? = dbQuery {
        val changed = InvoicesTable.update({ InvoicesTable.id eq id }) {
            it[InvoicesTable.filePath] = filePath
        }

        if (changed == 0) {
            null
        } else {
            val row = InvoicesTable.selectAll().where { InvoicesTable.id eq id }.single()
            toInvoice(row, getLines(id))
        }
    }

    suspend fun deleteInvoice(id: String): Boolean = dbQuery {
        InvoiceLinesTable.deleteWhere { invoiceId eq id }
        InvoicesTable.deleteWhere { InvoicesTable.id eq id } > 0
    }

    private fun getLines(invoiceId: String): List<InvoiceLine> {
        return InvoiceLinesTable.selectAll()
            .where { InvoiceLinesTable.invoiceId eq invoiceId }
            .map(::toLine)
    }

    private fun toInvoice(row: ResultRow, lines: List<InvoiceLine>): Invoice {
        return Invoice(
            id = row[InvoicesTable.id],
            number = row[InvoicesTable.number],
            orderId = row[InvoicesTable.orderId],
            clientId = row[InvoicesTable.clientId],
            issuedAt = kotlin.time.Instant.fromEpochMilliseconds(row[InvoicesTable.issuedAt].toEpochMilli()),
            validUntil = row[InvoicesTable.validUntil]?.let { kotlin.time.Instant.fromEpochMilliseconds(it.toEpochMilli()) },
            payer = row[InvoicesTable.payer],
            orderRef = row[InvoicesTable.orderRef],
            seller = InvoiceSellerSnapshot(
                ownerName = row[InvoicesTable.sellerOwnerName],
                taxId = row[InvoicesTable.sellerTaxId],
                address = row[InvoicesTable.sellerAddress],
                iban = row[InvoicesTable.sellerIban],
                bankName = row[InvoicesTable.sellerBankName],
                taxPercent = row[InvoicesTable.sellerTaxPercent],
                taxNote = row[InvoicesTable.sellerTaxNote],
                mfo = row[InvoicesTable.sellerMfo],
                ipn = row[InvoicesTable.sellerIpn],
            ),
            client = InvoiceClientSnapshot(
                type = ClientType.valueOf(row[InvoicesTable.clientType]),
                name = row[InvoicesTable.clientName],
                address = row[InvoicesTable.clientAddress],
                phone = row[InvoicesTable.clientPhone],
                email = row[InvoicesTable.clientEmail]
            ),
            lines = lines,
            subtotal = row[InvoicesTable.subtotal],
            discountAmount = row[InvoicesTable.discountAmount],
            taxAmount = row[InvoicesTable.taxAmount],
            totalAmount = row[InvoicesTable.totalAmount],
            notes = row[InvoicesTable.notes],
            filePath = row[InvoicesTable.filePath]
        )
    }

    private fun toLine(row: ResultRow): InvoiceLine {
        return InvoiceLine(
            lineNumber = row[InvoiceLinesTable.lineNumber],
            description = row[InvoiceLinesTable.description],
            quantity = row[InvoiceLinesTable.quantity],
            unit = row[InvoiceLinesTable.unit],
            usedMeters = row[InvoiceLinesTable.usedMeters],
            unitPrice = row[InvoiceLinesTable.unitPrice],
            lineTotal = row[InvoiceLinesTable.lineTotal]
        )
    }
}