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
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.printbusinesskmp.models.Invoice
import com.printbusinesskmp.utils.FormatUtils
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Path

/**
 * Видаткова накладна (goods-issue note) PDF, built from the same [Invoice] data
 * as the invoice but with its own number and "Відпустив / Отримав" signatures.
 */
object DesktopDeliveryNotePdfGenerator {

    private val ink = DeviceRgb(10, 48, 45)
    private val muted = DeviceRgb(23, 111, 104)
    private val faint = DeviceRgb(32, 159, 149)
    private val lineColor = DeviceRgb(226, 232, 240)
    private val accent = DeviceRgb(48, 213, 200)

    fun generate(invoice: Invoice, deliveryNoteNumber: String, destination: Path) {
        val writer = PdfWriter(destination.toString())
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(36f, 40f, 36f, 40f)

        val font = loadFont("DejaVuSans.ttf")
        val bold = loadFont("DejaVuSans-Bold.ttf") ?: font
        document.setFont(font)
        document.setFontColor(ink)

        // ── Text styles (single source of truth) ───────────────────────────────
        fun partyLabel(text: String) = Paragraph(text)
            .setFont(bold).setFontSize(11f).setCharacterSpacing(1f).setMarginBottom(2f)

        fun partyName(text: String) = Paragraph(text)
            .setFont(bold).setFontSize(11f).setMarginBottom(2f)

        fun partyLine(text: String) = Paragraph(text)
            .setFont(font).setFontSize(8.5f).setMultipliedLeading(1.25f).setMargin(0f)

        fun docTitle(text: String) = Paragraph(text)
            .setFont(bold).setFontSize(17f).setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(14f).setMarginBottom(0f)

        fun docSubtitle(text: String) = Paragraph(text)
            .setFont(font).setFontSize(9.5f)
//            .setFontColor(muted)
            .setTextAlignment(TextAlignment.CENTER).setMarginTop(2f).setMarginBottom(14f)

        fun headerText(text: String, align: TextAlignment) = Paragraph(text)
            .setFont(bold).setFontSize(8.5f)
//            .setFontColor(ink)
            .setTextAlignment(align)

        fun cellText(text: String, align: TextAlignment) = Paragraph(text)
            .setFont(font).setFontSize(9f).setTextAlignment(align)

        fun signatureTitle(text: String) = Paragraph(text)
            .setFont(bold).setFontSize(9.5f).setMarginBottom(14f)

        fun signatureName(text: String) = Paragraph(text)
            .setFont(font).setFontSize(8.5f)
//            .setFontColor(muted)
            .setWidth(UnitValue.createPercentValue(80f))
            .setBorderTop(SolidBorder(faint, 0.7f)).setPaddingTop(3f).setMargin(0f)

        fun sumInWords(prefix: String, words: String) = Paragraph()
            .add(Text(prefix).setFont(font)
//                .setFontColor(muted)
            )
            .add(Text(words).setFont(bold))
            .setFontSize(9.5f).setMarginTop(10f).setMarginBottom(0f)

        fun taxNoteText(text: String) = Paragraph(text)
            .setFont(font).setFontSize(7.5f)
//            .setFontColor(faint)
            .setMarginTop(14f).setBorderTop(SolidBorder(lineColor, 0.5f)).setPaddingTop(5f)

        // ── 1. Header: supplier (left) + recipient (right) ──────────────────────
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 20f, 40f)))
            .setWidth(UnitValue.createPercentValue(100f))

        val supplierCell = Cell().setBorder(Border.NO_BORDER).setPaddingRight(14f)
        supplierCell.add(partyLabel("ПОСТАЧАЛЬНИК"))
        supplierCell.add(partyName("ФОП ${invoice.seller.ownerName}"))
        supplierCell.add(partyLine("ЄДРПОУ: ${invoice.seller.taxId}"))
        invoice.seller.ipn?.takeIf { it.isNotBlank() }?.let { supplierCell.add(partyLine("ІПН: $it")) }
        supplierCell.add(partyLine("IBAN: ${invoice.seller.iban}"))
        val bankLine = buildString {
            if (invoice.seller.bankName.isNotBlank()) append(invoice.seller.bankName)
            invoice.seller.mfo?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append(", МФО: $it") else append("МФО: $it")
            }
        }
        if (bankLine.isNotBlank()) supplierCell.add(partyLine("Банк: $bankLine"))
        invoice.seller.bankEdrpou?.takeIf { it.isNotBlank() }?.let { supplierCell.add(partyLine("ЄДРПОУ банку: $it")) }
        if (invoice.seller.address.isNotBlank()) supplierCell.add(partyLine("Адреса: ${invoice.seller.address}"))

        val recipientCell = Cell().setBorder(Border.NO_BORDER)
        recipientCell.add(partyLabel("ОДЕРЖУВАЧ"))
        recipientCell.add(partyName(invoice.client.name))
        if (invoice.client.phone.isNotBlank()) recipientCell.add(partyLine("тел. ${invoice.client.phone}"))
        if (invoice.client.address.isNotBlank()) recipientCell.add(partyLine("Адреса: ${invoice.client.address}"))
        invoice.client.email?.takeIf { it.isNotBlank() }?.let { recipientCell.add(partyLine("Email: $it")) }
        recipientCell.add(partyLine("Платник: ${invoice.payer}"))

        headerTable.addCell(supplierCell)
        headerTable.addCell(Cell().setBorder(Border.NO_BORDER))
        headerTable.addCell(recipientCell)
        document.add(headerTable)

        // ── 2. Accent divider + title ────────────────────────────────────────────
        document.add(
            Paragraph().setMarginTop(10f).setMarginBottom(0f)
                .setBorderBottom(SolidBorder(accent, 1.6f)).setHeight(1f)
        )

        document.add(docTitle("Видаткова накладна № $deliveryNoteNumber"))
        document.add(docSubtitle("від ${formatLongDate(invoice.issuedAt)} р.  ·  до рахунку № ${invoice.number}"))

        // ── 3. Items table ───────────────────────────────────────────────────────
        val itemsTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.5f, 4.4f, 0.7f, 0.9f, 1.4f, 1.4f))
        ).setWidth(UnitValue.createPercentValue(100f))

        fun hdr(text: String, align: TextAlignment = TextAlignment.CENTER): Cell =
            Cell().add(headerText(text, align))
                .setBackgroundColor(accent).setPadding(5f).setBorder(Border.NO_BORDER)

        itemsTable.addHeaderCell(hdr("№"))
        itemsTable.addHeaderCell(hdr("Назва", TextAlignment.LEFT))
        itemsTable.addHeaderCell(hdr("Од."))
        itemsTable.addHeaderCell(hdr("К-сть"))
        itemsTable.addHeaderCell(hdr("Ціна без ПДВ", TextAlignment.RIGHT))
        itemsTable.addHeaderCell(hdr("Сума без ПДВ", TextAlignment.RIGHT))

        invoice.lines.forEach { line ->
            fun dat(text: String, align: TextAlignment = TextAlignment.CENTER): Cell =
                Cell().add(cellText(text, align))
                    .setPadding(5f).setBorder(Border.NO_BORDER)
                    .setBorderBottom(SolidBorder(lineColor, 0.5f))

            itemsTable.addCell(dat(line.lineNumber.toString()))
            itemsTable.addCell(dat(line.description, TextAlignment.LEFT))
            itemsTable.addCell(dat(line.unit))
            itemsTable.addCell(dat(line.quantity.toString()))
            itemsTable.addCell(dat(fmt(line.unitPrice), TextAlignment.RIGHT))
            itemsTable.addCell(dat(fmt(line.lineTotal), TextAlignment.RIGHT))
        }

        document.add(itemsTable)

        // ── 4. Totals summary + sum in words ─────────────────────────────────────
        val itemsCount = invoice.lines.size
        document.add(
            Paragraph("Всього найменувань $itemsCount, на суму ${fmt(invoice.totalAmount)} грн")
                .setFont(font).setFontSize(9.5f).setMarginTop(10f).setMarginBottom(0f)
        )
        document.add(sumInWords("Сума прописом: ", FormatUtils.amountInUkrainianWords(invoice.totalAmount)))
        document.add(
            Paragraph("У тому числі ПДВ: 0.00 грн.")
                .setFont(font).setFontSize(9f)
//                .setFontColor(muted)
                .setMarginTop(1f)
        )

        // ── 6. Signatures: released (left) + received (right) ────────────────────
        val signTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 0.15f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f)).setMarginTop(34f)

        val releasedCell = Cell().setBorder(Border.NO_BORDER)
        releasedCell.add(signatureTitle("Відпустив(ла):"))
        releasedCell.add(signatureName("ФОП ${invoice.seller.ownerName}"))

        val receivedCell = Cell().setBorder(Border.NO_BORDER)
        receivedCell.add(signatureTitle("Отримав(ла):"))
        receivedCell.add(signatureName(" "))

        signTable.addCell(releasedCell)
        signTable.addCell(Cell().setBorder(Border.NO_BORDER))
        signTable.addCell(receivedCell)
        document.add(signTable)

        val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
            ?: "Не є платником податку на прибуток на загальних підставах"
        document.add(taxNoteText(taxNote))

        document.close()
    }

    private fun loadFont(fileName: String): PdfFont? {
        runCatching {
            val bytes = DesktopDeliveryNotePdfGenerator::class.java.classLoader
                .getResourceAsStream("fonts/$fileName")?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

        val devPath = File("backend/src/main/resources/fonts/$fileName")
        if (devPath.exists()) {
            runCatching {
                return PdfFontFactory.createFont(devPath.absolutePath, PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
            }
        }

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

        return runCatching { PdfFontFactory.createFont(StandardFonts.HELVETICA) }.getOrNull()
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

    private fun fmt(value: Double): String = String.format("%.2f", value)
}
