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

    private val ink = DeviceRgb(30, 41, 59)        // основний тёмний (slate-800)
    private val muted = DeviceRgb(100, 116, 139)   // другорядний текст (slate-500)
    private val faint = DeviceRgb(148, 163, 184)   // дрібні підписи (slate-400)
    private val lineColor = DeviceRgb(226, 232, 240) // роздільники (slate-200)
    private val zebraBg = DeviceRgb(248, 250, 252) // парні рядки (slate-50)

    fun generate(invoice: Invoice, destination: Path) {
        val writer = PdfWriter(destination.toString())
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(36f, 40f, 36f, 40f)

        val font = loadFont("DejaVuSans.ttf")
        val bold = loadFont("DejaVuSans-Bold.ttf") ?: font
        document.setFont(font)
        document.setFontColor(ink)

        // ── 1. Header: supplier (top-left) + recipient (top-right) ──────────────
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        fun partyLabel(text: String) = Paragraph(text)
            .setFont(bold).setFontSize(8f).setFontColor(faint)
            .setCharacterSpacing(1f)
            .setMarginBottom(2f)

        fun partyName(text: String) = Paragraph(text)
            .setFont(bold).setFontSize(11f)
            .setMarginBottom(2f)

        fun partyLine(text: String) = Paragraph(text)
            .setFont(font).setFontSize(8.5f).setFontColor(muted)
            .setMultipliedLeading(1.25f)
            .setMargin(0f)

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
        if (bankLine.isNotBlank()) supplierCell.add(partyLine(bankLine))
        supplierCell.add(partyLine(invoice.seller.address))

        val recipientCell = Cell().setBorder(Border.NO_BORDER).setPaddingLeft(14f)
        recipientCell.add(partyLabel("ОДЕРЖУВАЧ"))
        recipientCell.add(partyName(invoice.client.name))
        if (invoice.client.phone.isNotBlank()) recipientCell.add(partyLine("тел. ${invoice.client.phone}"))
        if (invoice.client.address.isNotBlank()) recipientCell.add(partyLine(invoice.client.address))
        invoice.client.email?.takeIf { it.isNotBlank() }?.let { recipientCell.add(partyLine(it)) }
        recipientCell.add(partyLine("Платник: ${invoice.payer}"))

        headerTable.addCell(supplierCell)
        headerTable.addCell(recipientCell)
        document.add(headerTable)

        // ── 2. Accent divider + title ────────────────────────────────────────────
        document.add(
            Paragraph().setMarginTop(10f).setMarginBottom(0f)
                .setBorderBottom(SolidBorder(ink, 1.2f))
                .setHeight(1f)
        )

        document.add(
            Paragraph("Рахунок-фактура № ${invoice.number}")
                .setFont(bold).setFontSize(17f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(14f).setMarginBottom(0f)
        )
        document.add(
            Paragraph("від ${formatLongDate(invoice.issuedAt)} р.  ·  ${invoice.orderRef}")
                .setFont(font).setFontSize(9.5f).setFontColor(muted)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2f).setMarginBottom(14f)
        )

        // ── 3. Items table ───────────────────────────────────────────────────────
        val itemsTable = Table(
            UnitValue.createPercentArray(floatArrayOf(0.5f, 4.4f, 0.7f, 0.9f, 1.4f, 1.4f))
        ).setWidth(UnitValue.createPercentValue(100f))

        fun hdr(text: String, align: TextAlignment = TextAlignment.CENTER): Cell =
            Cell().add(
                Paragraph(text).setFont(bold).setFontSize(8.5f)
                    .setFontColor(DeviceRgb(255, 255, 255))
                    .setTextAlignment(align)
            ).setBackgroundColor(ink).setPadding(5f).setBorder(Border.NO_BORDER)

        itemsTable.addHeaderCell(hdr("№"))
        itemsTable.addHeaderCell(hdr("Назва", TextAlignment.LEFT))
        itemsTable.addHeaderCell(hdr("Од."))
        itemsTable.addHeaderCell(hdr("К-сть"))
        itemsTable.addHeaderCell(hdr("Ціна без ПДВ", TextAlignment.RIGHT))
        itemsTable.addHeaderCell(hdr("Сума без ПДВ", TextAlignment.RIGHT))

        invoice.lines.forEachIndexed { index, line ->
            fun dat(text: String, align: TextAlignment = TextAlignment.CENTER): Cell {
                val cell = Cell().add(
                    Paragraph(text).setFont(font).setFontSize(9f).setTextAlignment(align)
                ).setPadding(5f).setBorder(Border.NO_BORDER)
                    .setBorderBottom(SolidBorder(lineColor, 0.5f))
                if (index % 2 == 1) cell.setBackgroundColor(zebraBg)
                return cell
            }

            itemsTable.addCell(dat(line.lineNumber.toString()))
            itemsTable.addCell(dat(line.description, TextAlignment.LEFT))
            itemsTable.addCell(dat(line.unit))
            itemsTable.addCell(dat(line.quantity.toString()))
            itemsTable.addCell(dat(fmt(line.unitPrice), TextAlignment.RIGHT))
            itemsTable.addCell(dat(fmt(line.lineTotal), TextAlignment.RIGHT))
        }

        document.add(itemsTable)

        // ── 4. Totals (borderless, right-aligned) ────────────────────────────────
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 2f)))
            .setWidth(UnitValue.createPercentValue(40f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setMarginTop(8f)

        fun totalRow(label: String, value: String, emphasized: Boolean = false) {
            val f = if (emphasized) bold else font
            val size = if (emphasized) 11f else 9.5f
            val color = if (emphasized) ink else muted
            val labelCell = Cell().add(
                Paragraph(label).setFont(f).setFontSize(size).setFontColor(color)
            ).setPadding(3f).setBorder(Border.NO_BORDER)
            val valueCell = Cell().add(
                Paragraph(value).setFont(f).setFontSize(size).setFontColor(ink)
                    .setTextAlignment(TextAlignment.RIGHT)
            ).setPadding(3f).setBorder(Border.NO_BORDER)
            if (emphasized) {
                labelCell.setBorderTop(SolidBorder(ink, 1f)).setPaddingTop(5f)
                valueCell.setBorderTop(SolidBorder(ink, 1f)).setPaddingTop(5f)
            }
            totalsTable.addCell(labelCell)
            totalsTable.addCell(valueCell)
        }

        if (invoice.discountAmount > 0.0) totalRow("Знижка:", "${fmt(invoice.discountAmount)} грн")
        totalRow("Разом без ПДВ:", "${fmt(invoice.subtotal)} грн")
        totalRow("ПДВ:", "0.00 грн")
        totalRow("Всього з ПДВ:", "${fmt(invoice.totalAmount)} грн", emphasized = true)

        document.add(totalsTable)

        // ── 5. Sum in words ──────────────────────────────────────────────────────
        document.add(
            Paragraph()
                .add(com.itextpdf.layout.element.Text("Всього на суму: ").setFont(font).setFontColor(muted))
                .add(com.itextpdf.layout.element.Text(FormatUtils.amountInUkrainianWords(invoice.totalAmount)).setFont(bold))
                .setFontSize(9.5f).setMarginTop(10f).setMarginBottom(0f)
        )
        document.add(
            Paragraph("У тому числі ПДВ: 0.00 грн.")
                .setFont(font).setFontSize(9f).setFontColor(muted)
                .setMarginTop(1f)
        )

        // ── 6. Footer: signature (left) + valid-until (right) ────────────────────
        val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(28f)

        val signatureCell = Cell().setBorder(Border.NO_BORDER)
        signatureCell.add(
            Paragraph("Виписав(ла):").setFont(bold).setFontSize(9.5f).setMarginBottom(14f)
        )
        signatureCell.add(
            Paragraph("ФОП ${invoice.seller.ownerName}")
                .setFont(font).setFontSize(8.5f).setFontColor(muted)
                .setWidth(UnitValue.createPercentValue(70f))
                .setBorderTop(SolidBorder(faint, 0.7f))
                .setPaddingTop(3f).setMargin(0f)
        )

        val validUntilCell = Cell().setBorder(Border.NO_BORDER)
        invoice.validUntil?.let {
            validUntilCell.add(
                Paragraph("Рахунок дійсний до сплати до ${formatShortDate(it)}")
                    .setFont(font).setFontSize(9f).setFontColor(muted)
                    .setTextAlignment(TextAlignment.RIGHT)
            )
        }

        footerTable.addCell(signatureCell)
        footerTable.addCell(validUntilCell)
        document.add(footerTable)

        val taxNote = invoice.seller.taxNote?.takeIf { it.isNotBlank() }
            ?: "Не є платником податку на прибуток на загальних підставах"
        document.add(
            Paragraph(taxNote)
                .setFont(font).setFontSize(7.5f).setFontColor(faint)
                .setMarginTop(14f)
                .setBorderTop(SolidBorder(lineColor, 0.5f))
                .setPaddingTop(5f)
        )

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
}