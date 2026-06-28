package com.printbusinesskmp.desktop.platform

import com.printbusinesskmp.api.ApiClient
import com.printbusinesskmp.desktop.pdf.DesktopDeliveryNotePdfGenerator
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
    val enriched = enrichSellerFromProfile(invoice)
    withContext(Dispatchers.IO) {
        Files.createDirectories(destination.parent)
        DesktopInvoicePdfGenerator.generate(enriched, destination)
    }
    return destination
}

/**
 * Fills seller fields that may be missing from the stored invoice snapshot
 * (e.g. bank EDRPOU added after the invoice was created) from the current profile.
 */
private suspend fun enrichSellerFromProfile(invoice: Invoice): Invoice {
    val profile = runCatching { ApiClient.getBusinessProfile() }.getOrNull() ?: return invoice
    val bankEdrpou = profile.bankEdrpou?.takeIf { it.isNotBlank() }
        ?: invoice.seller.bankEdrpou
    return invoice.copy(seller = invoice.seller.copy(bankEdrpou = bankEdrpou))
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

/** Deterministic destination of a delivery-note PDF inside the configured folder. */
fun deliveryNoteFilePath(invoice: Invoice): Path =
    AppSettingsStore.invoicesDir.resolve(buildDeliveryNoteFileName(invoice))

/**
 * Effective date printed on (and used in the filename of) the delivery note: the
 * per-invoice override if the user set one, otherwise the invoice's issue date.
 */
fun deliveryNoteIssuedDate(invoice: Invoice): kotlin.time.Instant =
    AppSettingsStore.deliveryNoteDateMillis(invoice.id)
        ?.let { kotlin.time.Instant.fromEpochMilliseconds(it) }
        ?: invoice.issuedAt

/**
 * Generates (or regenerates) the delivery-note (видаткова накладна) PDF, allocating
 * the local "ВН-" number on first generation. Returns the written path.
 */
suspend fun generateDeliveryNoteToFolder(invoice: Invoice): Path {
    val destination = deliveryNoteFilePath(invoice)
    val enriched = enrichSellerFromProfile(invoice)
    val number = AppSettingsStore.deliveryNoteNumber(invoice.id)
    val issuedDate = deliveryNoteIssuedDate(invoice)
    withContext(Dispatchers.IO) {
        Files.createDirectories(destination.parent)
        DesktopDeliveryNotePdfGenerator.generate(enriched, number, destination, issuedDate)
    }
    return destination
}

/** Deletes the delivery-note PDF and forgets its number/date for the invoice. */
suspend fun deleteDeliveryNote(invoice: Invoice) {
    val path = deliveryNoteFilePath(invoice)
    withContext(Dispatchers.IO) { Files.deleteIfExists(path) }
    AppSettingsStore.removeDeliveryNote(invoice.id)
}

/**
 * Deletes the desktop PDF files associated with an invoice (the invoice itself and
 * its delivery note) and forgets the delivery-note mapping. The invoice record is
 * removed separately via the API.
 */
suspend fun deleteInvoiceDocuments(invoice: Invoice) {
    val invoicePath = invoiceFilePath(invoice)
    // Only resolve the note path if a ВН was actually issued — otherwise the
    // filename builder would allocate a fresh ВН number for nothing.
    val notePath = AppSettingsStore.existingDeliveryNoteNumber(invoice.id)
        ?.let { deliveryNoteFilePath(invoice) }
    withContext(Dispatchers.IO) {
        Files.deleteIfExists(invoicePath)
        notePath?.let { Files.deleteIfExists(it) }
    }
    AppSettingsStore.removeDeliveryNote(invoice.id)
}

/**
 * Opens the delivery-note PDF from the configured folder. Returns false if missing.
 */
suspend fun openDeliveryNoteFromFolder(invoice: Invoice): Boolean {
    val path = deliveryNoteFilePath(invoice)
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

fun buildDeliveryNoteFileName(invoice: Invoice): String {
    val number = AppSettingsStore.deliveryNoteNumber(invoice.id)
    val safeClient = invoice.client.name
        .replace(Regex("[^A-Za-zА-Яа-яёЁіІїЇєЄ0-9]"), "_")
        .trimEnd('_')
        .take(40)
    val dt = deliveryNoteIssuedDate(invoice).toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "${dt.year}-${dt.month.number.toString().padStart(2, '0')}-${
        dt.day.toString().padStart(2, '0')
    }"
    return "${number}_${safeClient}_$date.pdf"
}
