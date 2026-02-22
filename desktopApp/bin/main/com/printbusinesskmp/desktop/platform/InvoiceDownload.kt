package com.printbusinesskmp.desktop.platform

import com.printbusinesskmp.api.ApiClient
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun saveInvoicePdf(invoiceId: String, invoiceNumber: String): Path? {
    val destination = choosePdfSavePath("$invoiceNumber.pdf") ?: return null
    val pdfBytes = ApiClient.downloadInvoicePdf(invoiceId)

    withContext(Dispatchers.IO) {
        destination.parent?.let { Files.createDirectories(it) }
        Files.write(destination, pdfBytes)
    }

    return destination
}
