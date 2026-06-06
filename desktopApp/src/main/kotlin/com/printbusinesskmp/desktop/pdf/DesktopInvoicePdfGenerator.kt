package com.printbusinesskmp.desktop.pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Path

object DesktopInvoicePdfGenerator {

    private val headerBg = DeviceRgb(0xE2.toFloat() / 255f, 0xE8.toFloat() / 255f, 0xF0.toFloat() / 255f)
    private val thinBorder = SolidBorder(0.5f)

    fun generate(invoice: Invoice, destination: Path) {
        val writer = PdfWriter(destination.toString())
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(28f, 28f, 28f, 28f)

        val font = loadFont("DejaVuSans.ttf")
        val bold = loadFont("DejaVuSans-Bold.ttf") ?: font
        document.setFont(font)

        // ── 1. Supplier block (left) + empty right ───────────────────────────────
        val headerTable = Table(floatArrayOf(1f, 1f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)

        val supplierText = buildString {
            appendLine("Постачальник:")
            appendLine("ФОП ${invoice.seller.ownerName}")
            appendLine("ЄДРПОУ: ${invoice.seller.taxId}")
            appendLine("IBAN: ${invoice.seller.iban}")
            val bankLine = buildString {
                if (invoice.seller.bankName.isNotBlank()) append(invoice.seller.bankName)
                invoice.seller.mfo?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(", МФО: $it") else append("МФО: $it")
                }
            }
            if (bankLine.isNotBlank()) appendLine(bankLine)
            invoice.seller.ipn?.takeIf { it.isNotBlank() }?.let { appendLine("ІПН: $it") }
            val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
                ?: "Не є платником податку на прибуток на загальних підставах"
            appendLine(taxNote)
            append("Адреса: ${invoice.seller.address}")
        }

        headerTable.addCell(
            Cell().add(Paragraph(supplierText).setFont(font).setFontSize(9f))
                .setBorder(null).setPaddingBottom(6f)
        )
        headerTable.addCell(Cell().setBorder(null))
        document.add(headerTable)

        document.add(gap(4f))

        // ── 2-4. Одержувач / Платник / Замовлення ──────────────────────────────
        val recipientTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 8f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)

        fun labelCell(text: String) =
            Cell().add(Paragraph(text).setFont(bold).setFontSize(9f))
                .setBorder(null).setPaddingRight(6f).setPaddingBottom(3f)

        fun valueCell(text: String) =
            Cell().add(Paragraph(text).setFont(font).setFontSize(9f))
                .setBorder(null).setPaddingBottom(3f)

        val clientDisplay = buildString {
            append(invoice.client.name)
            if (invoice.client.phone.isNotBlank()) append(", тел. ${invoice.client.phone}")
        }

        recipientTable.addCell(labelCell("Одержувач:"))
        recipientTable.addCell(valueCell(clientDisplay))
        recipientTable.addCell(labelCell("Платник:"))
        recipientTable.addCell(valueCell(invoice.payer))
        recipientTable.addCell(labelCell("Замовлення:"))
        recipientTable.addCell(valueCell(invoice.orderRef))
        document.add(recipientTable)

        document.add(gap(8f))

        // ── 5-6. Invoice title + date (centered) ────────────────────────────────
        document.add(
            Paragraph("Рахунок-фактура № ${invoice.number}")
                .setFont(bold).setFontSize(15f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        document.add(
            Paragraph("від ${formatLongDate(invoice.issuedAt)} р.")
                .setFont(font).setFontSize(11f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(gap(8f))

        // ── 7. Items table ───────────────────────────────────────────────────────
        val itemsTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.5f, 4f, 0.7f, 0.8f, 1.4f, 1.4f))
        ).setWidth(UnitValue.createPercentValue(100f))

        fun hdr(text: String, align: TextAlignment = TextAlignment.CENTER): Cell =
            Cell().add(Paragraph(text).setFont(bold).setFontSize(9f).setTextAlignment(align))
                .setBackgroundColor(headerBg).setPadding(4f).setBorder(thinBorder)

        itemsTable.addHeaderCell(hdr("№"))
        itemsTable.addHeaderCell(hdr("Назва", TextAlignment.LEFT))
        itemsTable.addHeaderCell(hdr("Од."))
        itemsTable.addHeaderCell(hdr("Кількість"))
        itemsTable.addHeaderCell(hdr("Ціна без ПДВ", TextAlignment.RIGHT))
        itemsTable.addHeaderCell(hdr("Сума без ПДВ", TextAlignment.RIGHT))

        invoice.lines.forEach { line ->
            fun dat(text: String, align: TextAlignment = TextAlignment.CENTER): Cell =
                Cell().add(Paragraph(text).setFont(font).setFontSize(9f).setTextAlignment(align))
                    .setPadding(4f).setBorder(thinBorder)

            itemsTable.addCell(dat(line.lineNumber.toString()))
            itemsTable.addCell(dat(line.description, TextAlignment.LEFT))
            itemsTable.addCell(dat(line.unit))
            itemsTable.addCell(dat(line.quantity.toString()))
            itemsTable.addCell(dat(fmt(line.unitPrice), TextAlignment.RIGHT))
            itemsTable.addCell(dat(fmt(line.lineTotal), TextAlignment.RIGHT))
        }

        document.add(itemsTable)

        document.add(gap(6f))

        // ── 8. Totals (right-aligned) ────────────────────────────────────────────
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1.8f)))
            .setWidth(UnitValue.createPercentValue(42f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)

        fun totalRow(label: String, value: String, isBold: Boolean = false) {
            val f = if (isBold) bold else font
            totalsTable.addCell(
                Cell().add(Paragraph(label).setFont(f).setFontSize(10f))
                    .setPadding(2f).setBorder(thinBorder)
            )
            totalsTable.addCell(
                Cell().add(Paragraph(value).setFont(f).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT))
                    .setPadding(2f).setBorder(thinBorder)
            )
        }

        if (invoice.discountAmount > 0.0) totalRow("Знижка:", "${fmt(invoice.discountAmount)} грн")
        totalRow("Разом без ПДВ:", "${fmt(invoice.subtotal)} грн")
        totalRow("ПДВ:", "0.00 грн")
        totalRow("Всього з ПДВ:", "${fmt(invoice.totalAmount)} грн", isBold = true)

        document.add(totalsTable)

        document.add(gap(6f))

        // ── 9-10. Sum in words ───────────────────────────────────────────────────
        document.add(
            Paragraph("Всього на суму: ${FormatUtils.amountInUkrainianWords(invoice.totalAmount)}")
                .setFont(font).setFontSize(10f)
        )
        document.add(Paragraph("ПДВ: 0.00 грн.").setFont(font).setFontSize(10f))

        document.add(gap(10f))

        // ── 11. Signature line ───────────────────────────────────────────────────
        val sigTable = Table(floatArrayOf(1f, 1f))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(null)

        sigTable.addCell(
            Cell().add(
                Paragraph("Виписав(ла): ФОП ${invoice.seller.ownerName}")
                    .setFont(bold).setFontSize(10f)
            ).setBorder(null)
        )

        // Valid-until date (bottom right)
        val validUntilText = invoice.validUntil
            ?.let { "Рахунок дійсний до сплати до ${formatShortDate(it)}" }
            ?: ""
        sigTable.addCell(
            Cell().add(
                Paragraph(validUntilText).setFont(font).setFontSize(9f)
                    .setTextAlignment(TextAlignment.RIGHT)
            ).setBorder(null)
        )

        document.add(sigTable)

        // Signature underline
        document.add(
            Paragraph("_________________________")
                .setFont(font).setFontSize(10f).setFontColor(DeviceRgb(0x94.toFloat() / 255f, 0xA3.toFloat() / 255f, 0xB8.toFloat() / 255f))
        )

        val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
            ?: "Не є платником податку на прибуток на загальних підставах"
        document.add(Paragraph(taxNote).setFont(font).setFontSize(8f).setFontColor(DeviceRgb(0x64.toFloat() / 255f, 0x74.toFloat() / 255f, 0x8B.toFloat() / 255f)))

        document.close()
    }

    private fun loadFont(fileName: String): PdfFont? {
        // 1) classpath resource (bundled in desktopApp)
        runCatching {
            val bytes = DesktopInvoicePdfGenerator::class.java.classLoader
                .getResourceAsStream("fonts/$fileName")?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        // 2) backend resources (dev mode — running from project root)
        val devPath = File("backend/src/main/resources/fonts/$fileName")
        if (devPath.exists()) {
            runCatching {
                return PdfFontFactory.createFont(devPath.absolutePath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        // 3) system fonts
        val systemCandidates = listOf(
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/Library/Fonts/Arial Unicode.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        )
        for (path in systemCandidates) {
            val f = File(path)
            if (!f.exists()) continue
            runCatching {
                return PdfFontFactory.createFont(f.absolutePath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }.getOrNull()
        }

        // 4) built-in fallback (no Cyrillic but PDF will not crash)
        return runCatching {
            PdfFontFactory.createFont(StandardFonts.HELVETICA)
        }.getOrNull()
    }

    private fun formatLongDate(instant: kotlin.time.Instant): String {
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val months = mapOf(
            1 to "січня", 2 to "лютого", 3 to "березня", 4 to "квітня",
            5 to "травня", 6 to "червня", 7 to "липня", 8 to "серпня",
            9 to "вересня", 10 to "жовтня", 11 to "листопада", 12 to "грудня"
        )
        return "${dt.day} ${months[dt.month.number] ?: dt.month.number} ${dt.year}"
    }

    private fun formatShortDate(instant: kotlin.time.Instant): String {
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.day.toString().padStart(2, '0')}.${dt.month.number.toString().padStart(2, '0')}.${dt.year.toString().takeLast(2)}"
    }

    private fun fmt(value: Double): String = String.format("%.2f", value)

    private fun gap(size: Float): Paragraph =
        Paragraph("\n").setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)).setFontSize(size / 2f)
}
