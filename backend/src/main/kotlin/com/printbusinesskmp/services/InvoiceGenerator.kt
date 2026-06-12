package com.printbusinesskmp.services

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.platform.AppDataPaths
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Files

class InvoiceGenerator {

    private val headerBg = DeviceRgb(0xE2.toFloat() / 255f, 0xE8.toFloat() / 255f, 0xF0.toFloat() / 255f)
    private val thinBorder = SolidBorder(0.5f)

    fun generateInvoicePdf(invoice: Invoice): String {
        val invoicesDir = AppDataPaths.resolved.invoiceDir
        Files.createDirectories(invoicesDir)

        val safeNumber = invoice.number.replace(Regex("[^A-Za-zА-Яа-яёЁіІїЇєЄ0-9_-]"), "_")
        val filePath = invoicesDir.resolve("$safeNumber.pdf").toAbsolutePath().normalize().toString()

        val writer = PdfWriter(filePath)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        val font = loadUkrainianFont()
        val boldFont = loadUkrainianBoldFont()
        document.setFont(font)

        // ── Header ──────────────────────────────────────────────────────────────
        document.add(
            Paragraph("Рахунок-фактура № ${invoice.number} від ${formatLongDate(invoice.issuedAt)} р.")
                .setFont(boldFont)
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(Paragraph("\n").setFontSize(3f))

        // ── Parties table ────────────────────────────────────────────────────────
        val parties = Table(floatArrayOf(1f, 0.5f, 1f)).setWidth(UnitValue.createPercentValue(100f))

        val sellerBlock = buildString {
            appendLine("Постачальник:")
            appendLine("ФОП ${invoice.seller.ownerName}")
            appendLine("ЄДРПОУ: ${invoice.seller.taxId}")
            appendLine("Адреса: ${invoice.seller.address}")
            appendLine("IBAN: ${invoice.seller.iban}")
            val bankInfo = buildString {
                if (invoice.seller.bankName.isNotBlank()) append(invoice.seller.bankName)
                invoice.seller.mfo?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(", МФО: $it") else append("МФО: $it") }
            }
            if (bankInfo.isNotBlank()) appendLine(bankInfo)
            val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
                ?: "Не є платником податку на прибуток на загальних підставах"
            append(taxNote)
        }

        val clientBlock = buildString {
            appendLine("Одержувач:")
            appendLine(invoice.client.name)
            appendLine("Телефон: ${invoice.client.phone}")
            invoice.client.email?.takeIf { it.isNotBlank() }?.let { appendLine("Email: $it") }
            appendLine()
            appendLine("Платник: ${invoice.payer}")
            append("Замовлення: ${invoice.orderRef}")
        }

        parties.addCell(Cell().add(Paragraph(sellerBlock).setFont(font).setFontSize(9f)).setPadding(10f))
        parties.addCell(Cell().setBorder(Border.NO_BORDER))
        parties.addCell(Cell().add(Paragraph(clientBlock).setFont(font).setFontSize(9f)).setPadding(10f))
        document.add(parties)

        document.add(Paragraph("\n").setFontSize(3f))

        // ── Line items table ─────────────────────────────────────────────────────
        val itemsTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.5f, 4f, 0.8f, 0.8f, 1.5f, 1.5f))
        ).setWidth(UnitValue.createPercentValue(100f))

        fun hdrCell(text: String): Cell =
            Cell().add(Paragraph(text).setFont(boldFont).setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(headerBg)
                .setPadding(4f)
                .setBorder(thinBorder)

        itemsTable.addHeaderCell(hdrCell("№"))
        itemsTable.addHeaderCell(hdrCell("Назва"))
        itemsTable.addHeaderCell(hdrCell("Од."))
        itemsTable.addHeaderCell(hdrCell("Кількість"))
        itemsTable.addHeaderCell(hdrCell("Ціна без ПДВ"))
        itemsTable.addHeaderCell(hdrCell("Сума без ПДВ"))

        invoice.lines.forEach { line ->
            fun dataCell(text: String, align: TextAlignment = TextAlignment.LEFT): Cell =
                Cell().add(Paragraph(text).setFont(font).setFontSize(9f).setTextAlignment(align))
                    .setPadding(4f).setBorder(thinBorder)

            itemsTable.addCell(dataCell(line.lineNumber.toString(), TextAlignment.CENTER))
            itemsTable.addCell(dataCell(line.description))
            itemsTable.addCell(dataCell(line.unit, TextAlignment.CENTER))
            itemsTable.addCell(dataCell(line.quantity.toString(), TextAlignment.CENTER))
            itemsTable.addCell(dataCell(fmt(line.unitPrice), TextAlignment.RIGHT))
            itemsTable.addCell(dataCell(fmt(line.lineTotal), TextAlignment.RIGHT))
        }

        document.add(itemsTable)

        document.add(Paragraph("\n").setFontSize(3f))

        // ── Totals ───────────────────────────────────────────────────────────────
        val totalsTable = Table(floatArrayOf(3f, 1.5f))
            .setWidth(UnitValue.createPercentValue(45f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)

        fun totalCell(text: String, bold: Boolean = false, right: Boolean = false): Cell {
            val f = if (bold) boldFont else font
            val align = if (right) TextAlignment.RIGHT else TextAlignment.LEFT
            return Cell().add(Paragraph(text).setFont(f).setFontSize(10f).setTextAlignment(align))
                .setPadding(3f).setBorder(thinBorder)
        }

        totalsTable.addCell(totalCell("Разом без ПДВ:"))
        totalsTable.addCell(totalCell("${fmt(invoice.subtotal)} грн", right = true))
        if (invoice.discountAmount > 0.0) {
            totalsTable.addCell(totalCell("Знижка:"))
            totalsTable.addCell(totalCell("${fmt(invoice.discountAmount)} грн", right = true))
        }
        totalsTable.addCell(totalCell("ПДВ:"))
        totalsTable.addCell(totalCell("0.00 грн", right = true))
        totalsTable.addCell(totalCell("Всього з ПДВ:", bold = true))
        totalsTable.addCell(totalCell("${fmt(invoice.totalAmount)} грн", bold = true, right = true))

        document.add(totalsTable)

        document.add(Paragraph("\n").setFontSize(3f))

        // ── Sum in words ─────────────────────────────────────────────────────────
        document.add(
            Paragraph("Всього на суму: ${FormatUtils.amountInUkrainianWords(invoice.totalAmount)}")
                .setFont(font).setFontSize(10f)
        )
        document.add(Paragraph("ПДВ: 0.00 грн.").setFont(font).setFontSize(10f))

        document.add(Paragraph("\n").setFontSize(3f))

        // ── Footer ───────────────────────────────────────────────────────────────
//        invoice.validUntil?.let { until ->
//            document.add(
//                Paragraph("Рахунок дійсний до сплати до: ${formatShortDate(until)}")
//                    .setFont(font).setFontSize(10f)
//            )
//        }

        document.add(Paragraph("\n").setFontSize(3f))

        document.add(
            Paragraph("Виписав(ла): ФОП ${invoice.seller.ownerName}")
                .setFont(font).setFontSize(10f)
        )

        val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
            ?: "Не є платником податку на прибуток на загальних підставах"
        document.add(Paragraph(taxNote).setFont(font).setFontSize(9f).setFontColor(ColorConstants.GRAY))

        invoice.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            document.add(Paragraph("Примітка: $notes").setFont(font).setFontSize(9f))
        }

        document.close()
        return filePath
    }

    private fun loadUkrainianFont(): PdfFont {
        val bundled = runCatching {
            javaClass.classLoader.getResourceAsStream("fonts/DejaVuSans.ttf")?.use { it.readBytes() }
        }.getOrNull()

        if (bundled != null && bundled.isNotEmpty()) {
            runCatching {
                return PdfFontFactory.createFont(bundled, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        val localProjectFont = File("backend/src/main/resources/fonts/DejaVuSans.ttf")
        if (localProjectFont.exists()) {
            runCatching {
                return PdfFontFactory.createFont(localProjectFont.absolutePath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        for (path in systemFontCandidates) {
            val f = File(path)
            if (!f.exists()) continue
            runCatching {
                return PdfFontFactory.createFont(f.absolutePath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }.getOrNull()
        }

        return PdfFontFactory.createFont(StandardFonts.HELVETICA, PdfEncodings.WINANSI, EmbeddingStrategy.PREFER_NOT_EMBEDDED)
    }

    private fun loadUkrainianBoldFont(): PdfFont {
        val bundled = runCatching {
            javaClass.classLoader.getResourceAsStream("fonts/DejaVuSans-Bold.ttf")?.use { it.readBytes() }
        }.getOrNull()

        if (bundled != null && bundled.isNotEmpty()) {
            runCatching {
                return PdfFontFactory.createFont(bundled, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        val localProjectFont = File("backend/src/main/resources/fonts/DejaVuSans-Bold.ttf")
        if (localProjectFont.exists()) {
            runCatching {
                return PdfFontFactory.createFont(localProjectFont.absolutePath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        return loadUkrainianFont()
    }

    private fun formatLongDate(instant: kotlin.time.Instant): String {
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val monthName = ukrMonthGenitive[dt.month.number] ?: dt.month.number.toString()
        return "${dt.day} $monthName ${dt.year}"
    }

    private fun formatShortDate(instant: kotlin.time.Instant): String {
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.day.toString().padStart(2, '0')}.${dt.month.number.toString().padStart(2, '0')}.${dt.year.toString().takeLast(2)}"
    }

    private fun fmt(value: Double): String = String.format("%.2f", value)

    private val systemFontCandidates = listOf(
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial Unicode.ttf",
        "C:/Windows/Fonts/arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    )

    private val ukrMonthGenitive = mapOf(
        1 to "січня", 2 to "лютого", 3 to "березня", 4 to "квітня",
        5 to "травня", 6 to "червня", 7 to "липня", 8 to "серпня",
        9 to "вересня", 10 to "жовтня", 11 to "листопада", 12 to "грудня"
    )
}
