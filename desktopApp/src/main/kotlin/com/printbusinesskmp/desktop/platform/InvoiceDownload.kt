package com.printbusinesskmp.desktop.platform

import com.printbusinesskmp.desktop.pdf.DesktopInvoicePdfGenerator
import com.printbusinesskmp.models.Invoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path

/** Deterministic destination of an invoice PDF inside the configured folder. */
fun invoiceFilePath(invoice: Invoice): Path =
    AppSettingsStore.invoicesDir.resolve(buildInvoiceFileName(invoice))

/**
 * Generates (or regenerates) the invoice PDF into the configured invoices folder,
 * overwriting any existing file. Returns the written path.
 */
suspend fun generateInvoiceToFolder(invoice: Invoice): Path {
    val destination = invoiceFilePath(invoice)
    withContext(Dispatchers.IO) {
        Files.createDirectories(destination.parent)
        DesktopInvoicePdfGenerator.generate(invoice, destination)
    }
    return destination
}

/**
 * Opens the invoice PDF from the configured folder in the system viewer.
 * Returns false if the file does not exist (caller should show "file not found").
 */
suspend fun openInvoiceFromFolder(invoice: Invoice): Boolean {
    val path = invoiceFilePath(invoice)
    if (!Files.exists(path)) return false
    withContext(Dispatchers.IO) { openFile(path) }
    return true
}

/** Opens a file or folder with the OS default application. */
fun openFile(path: Path) {
    Desktop.getDesktop().open(path.toFile())
}

fun buildInvoiceFileName(invoice: Invoice): String {
    val safeClient = invoice.client.name
        .replace(Regex("[^A-Za-zА-Яа-яёЁіІїЇєЄ0-9]"), "_")
        .trimEnd('_')
        .take(40)
    val dt = invoice.issuedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "${dt.year}-${dt.month.number.toString().padStart(2, '0')}-${
        dt.day.toString().padStart(2, '0')
    }"
    return "${invoice.number}_${safeClient}_$date.pdf"
}
