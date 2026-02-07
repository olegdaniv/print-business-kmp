package com.printbusinesskmp.services

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.printbusinesskmp.models.Invoice
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.io.File

class InvoiceGenerator {

    fun generateInvoicePdf(invoice: Invoice): String {
        val invoicesDir = File("invoices")
        invoicesDir.mkdirs()

        val safeNumber = invoice.number.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val filePath = File(invoicesDir, "$safeNumber.pdf").absolutePath

        val writer = PdfWriter(filePath)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        val font = loadUkrainianFont()
        document.setFont(font)

        document.add(
            Paragraph("Рахунок ${invoice.number}")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(18f)
        )
        document.add(
            Paragraph("Дата: ${formatDate(invoice.issuedAt)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(11f)
        )
        document.add(Paragraph("\n"))

        document.add(Paragraph("Продавець (ФОП)").setBold())
        document.add(Paragraph(invoice.seller.ownerName))
        document.add(Paragraph("РНОКПП: ${invoice.seller.taxId}"))
        document.add(Paragraph("Адреса: ${invoice.seller.address}"))
        document.add(Paragraph("IBAN: ${invoice.seller.iban}"))
        document.add(Paragraph("Банк: ${invoice.seller.bankName}"))
        document.add(Paragraph("\n"))

        document.add(Paragraph("Покупець").setBold())
        document.add(Paragraph(invoice.client.name))
        document.add(Paragraph("Адреса: ${invoice.client.address}"))
        document.add(Paragraph("Телефон: ${invoice.client.phone}"))
        invoice.client.email?.let { email ->
            document.add(Paragraph("Email: $email"))
        }
        document.add(Paragraph("\n"))

        val table = Table(floatArrayOf(0.7f, 4f, 1f, 1.2f, 1.5f, 1.5f))
        table.setWidth(UnitValue.createPercentValue(100f))
        table.addHeaderCell("№")
        table.addHeaderCell("Послуга")
        table.addHeaderCell("К-сть")
        table.addHeaderCell("Метри")
        table.addHeaderCell("Ціна")
        table.addHeaderCell("Сума")

        invoice.lines.forEach { line ->
            table.addCell(line.lineNumber.toString())
            table.addCell(line.description)
            table.addCell(line.quantity.toString())
            table.addCell("%.2f".format(line.usedMeters))
            table.addCell("%.2f".format(line.unitPrice))
            table.addCell("%.2f".format(line.lineTotal))
        }

        document.add(table)
        document.add(Paragraph("\n"))

        document.add(
            Paragraph("Сума без податку: %.2f грн".format(invoice.subtotal))
                .setTextAlignment(TextAlignment.RIGHT)
        )
        document.add(
            Paragraph("Податок: %.2f грн".format(invoice.taxAmount))
                .setTextAlignment(TextAlignment.RIGHT)
        )
        document.add(
            Paragraph("До сплати: %.2f грн".format(invoice.totalAmount))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setFontSize(13f)
        )

        val taxNote = if (invoice.seller.taxPercent > 0.0) {
            "Податок ${invoice.seller.taxPercent}% застосовано до рахунку."
        } else {
            "Податок не застосовано."
        }
        document.add(Paragraph("\n"))
        document.add(Paragraph(taxNote))

        invoice.notes?.takeIf { it.isNotBlank() }?.let { note ->
            document.add(Paragraph("Примітки: $note"))
        }

        document.close()
        return filePath
    }

    private fun loadUkrainianFont(): PdfFont {
        val classpathFont = runCatching {
            javaClass.classLoader
                .getResourceAsStream("fonts/DejaVuSans.ttf")
                ?.use { stream -> stream.readBytes() }
        }.getOrNull()

        if (classpathFont != null && classpathFont.isNotEmpty()) {
            runCatching {
                return PdfFontFactory.createFont(
                    classpathFont,
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.PREFER_EMBEDDED
                )
            }
        }

        val localFont = runCatching {
            File("backend/src/main/resources/fonts/DejaVuSans.ttf")
                .takeIf { file -> file.exists() }
                ?.readBytes()
        }.getOrNull()

        if (localFont != null && localFont.isNotEmpty()) {
            runCatching {
                return PdfFontFactory.createFont(
                    localFont,
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.PREFER_EMBEDDED
                )
            }
        }

        return PdfFontFactory.createFont(
            StandardFonts.HELVETICA,
            PdfEncodings.WINANSI,
            EmbeddingStrategy.PREFER_NOT_EMBEDDED
        )
    }

    private fun formatDate(instant: kotlin.time.Instant): String {
        val converted = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        return "${converted.day.toString().padStart(2, '0')}." +
            "${converted.month.number.toString().padStart(2, '0')}.${converted.year}"
    }
}
