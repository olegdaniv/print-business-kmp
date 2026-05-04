package com.printbusinesskmp.desktop.platform

import com.printbusinesskmp.desktop.pdf.DesktopInvoicePdfGenerator
import com.printbusinesskmp.models.Invoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.nio.file.Path

suspend fun saveInvoicePdf(invoice: Invoice): Path? {
    val defaultName = buildFileName(invoice)
    val destination = choosePdfSavePath(defaultName) ?: return null

    withContext(Dispatchers.IO) {
        DesktopInvoicePdfGenerator.generate(invoice, destination)
    }

    return destination
}

private fun buildFileName(invoice: Invoice): String {
    val safeClient = invoice.client.name
        .replace(Regex("[^A-Za-zА-Яа-яёЁіІїЇєЄ0-9]"), "_")
        .trimEnd('_')
        .take(40)
    val dt = invoice.issuedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
    return "${invoice.number}_${safeClient}_$date.pdf"
}
