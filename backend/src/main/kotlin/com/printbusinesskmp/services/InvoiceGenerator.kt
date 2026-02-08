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

        document.setFont(loadUkrainianFont())

//        // Approval block
//        document.add(Paragraph("ЗАТВЕРДЖУЮ").setFontSize(10f))
//        document.add(Paragraph("ВИКОНАВЕЦЬ").setFontSize(10f))
//        document.add(Paragraph("Фізична особа-підприємець").setFontSize(10f))
//        document.add(Paragraph(invoice.seller.ownerName).setFontSize(10f))
//        document.add(Paragraph("________________________").setFontSize(10f))
//        document.add(Paragraph("(підпис)").setFontSize(8f))
//
//        document.add(Paragraph("\n"))

        // Act title
        document.add(
            Paragraph("АКТ наданих послуг")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16f)
        )
        document.add(
            Paragraph("№${invoice.number} від ${formatDate(invoice.issuedAt)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
        )

        document.add(Paragraph("\n"))

        // Intro
        document.add(
            Paragraph(
                "Я, що нижче підписався, Виконавець ФОП ${invoice.seller.ownerName}, " +
                    "(РНОКПП: ${invoice.seller.taxId}) підтверджую, що були надані такі послуги:"
            ).setFontSize(10f)
        )

        document.add(Paragraph("\n"))

        // Service table
        val table = Table(floatArrayOf(0.8f, 4.4f, 1.3f, 1.3f, 1.8f, 1.8f))
        table.setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell("№")
        table.addHeaderCell("Найменування послуг")
        table.addHeaderCell("Кількість")
        table.addHeaderCell("Одиниця")
        table.addHeaderCell("Ціна")
        table.addHeaderCell("Сума")

        invoice.lines.forEach { line ->
            table.addCell(line.lineNumber.toString())
            table.addCell(line.description)
            table.addCell(line.quantity.toString())
            table.addCell(line.unit)
            table.addCell(formatNumber(line.unitPrice))
            table.addCell(formatNumber(line.lineTotal))
        }

        document.add(table)

        document.add(Paragraph("\n"))

        // Totals
        document.add(
            Paragraph("Сума без податку: ${formatNumber(invoice.subtotal)} грн")
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11f)
        )
        document.add(
            Paragraph("Податок: ${formatNumber(invoice.taxAmount)} грн")
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11f)
        )
        document.add(
            Paragraph("Всього до сплати: ${formatNumber(invoice.totalAmount)} грн")
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setFontSize(12f)
        )

        document.add(Paragraph("\n"))
        document.add(Paragraph("Замовник претензій по об'єму, якості та строкам надання послуг не має.").setFontSize(10f))

        val taxNote = if (invoice.seller.taxPercent > 0.0) {
            "Податок ${formatNumber(invoice.seller.taxPercent)}% застосовано до рахунку."
        } else {
            "Податок не застосовано."
        }
        document.add(Paragraph(taxNote).setFontSize(10f))

        invoice.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            document.add(Paragraph("Примітки: $notes").setFontSize(10f))
        }

        document.add(Paragraph("\n"))
        document.add(Paragraph("Реквізити сторін").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(12f))

        // Requisites block
        val parties = Table(floatArrayOf(1f, 1f)).setWidth(UnitValue.createPercentValue(100f))

        val sellerBlock = listOf(
            "Виконавець:",
            "ФОП ${invoice.seller.ownerName}",
            "РНОКПП: ${invoice.seller.taxId}",
            "Адреса: ${invoice.seller.address}",
            "IBAN: ${invoice.seller.iban}",
            "Банк: ${invoice.seller.bankName}",
            "",
            "_____________________",
            "(підпис)"
        ).joinToString("\n")

        val buyerBlock = listOf(
            "Замовник:",
            invoice.client.name,
            "Адреса: ${invoice.client.address}",
            "Телефон: ${invoice.client.phone}",
            invoice.client.email?.let { "Email: $it" } ?: "",
            "",
            "_____________________",
            "(підпис)"
        ).joinToString("\n")

        parties.addCell(Paragraph(sellerBlock).setFontSize(10f))
        parties.addCell(Paragraph(buyerBlock).setFontSize(10f))

        document.add(parties)

        document.close()
        return filePath
    }

    private fun loadUkrainianFont(): PdfFont {
        // 1) bundled resource font
        val bundled = runCatching {
            javaClass.classLoader.getResourceAsStream("fonts/DejaVuSans.ttf")?.use { it.readBytes() }
        }.getOrNull()

        if (bundled != null && bundled.isNotEmpty()) {
            runCatching {
                return PdfFontFactory.createFont(
                    bundled,
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.PREFER_EMBEDDED
                )
            }
        }

        // 2) local project font (dev mode)
        val localProjectFont = File("backend/src/main/resources/fonts/DejaVuSans.ttf")
        if (localProjectFont.exists()) {
            runCatching {
                return PdfFontFactory.createFont(
                    localProjectFont.absolutePath,
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.PREFER_EMBEDDED
                )
            }
        }

        // 3) known system fonts with Cyrillic on macOS/Linux/Windows paths
        val systemCandidates = listOf(
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/Library/Fonts/Arial Unicode.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        )

        for (path in systemCandidates) {
            val fontFile = File(path)
            if (!fontFile.exists()) continue

            val loaded = runCatching {
                PdfFontFactory.createFont(
                    fontFile.absolutePath,
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.PREFER_EMBEDDED
                )
            }.getOrNull()

            if (loaded != null) return loaded
        }

        // 4) last resort: built-in font (document still generated)
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

    private fun formatNumber(value: Double): String {
        return String.format("%.2f", value)
    }
}
