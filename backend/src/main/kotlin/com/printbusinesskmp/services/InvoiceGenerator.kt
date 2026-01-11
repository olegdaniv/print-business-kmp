package com.printbusinesskmp.services

import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.printbusinesskmp.models.FopDetails
import com.printbusinesskmp.models.Invoice
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File

class InvoiceGenerator {

    private val fontPath = "backend/src/main/resources/fonts/DejaVuSans.ttf"

    fun generateInvoicePdf(invoice: Invoice, fopDetails: FopDetails): String {
        val fileName = "invoice_${invoice.number}_${System.currentTimeMillis()}.pdf"
        val invoicesDir = File("invoices")
        invoicesDir.mkdirs()
        val filePath = "invoices/$fileName"

        // Create PDF
        val pdfWriter = PdfWriter(filePath)
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc)

        // Load Ukrainian font (important for Cyrillic)
        val font: PdfFont = PdfFontFactory.createFont(fontPath, "Identity-H")
        document.setFont(font)

        // Header - Approval section
        document.add(Paragraph("ЗАТВЕРДЖУЮ").setTextAlignment(TextAlignment.LEFT).setFontSize(10f))
        document.add(Paragraph("ВИКОНАВЕЦЬ").setFontSize(10f))
        document.add(Paragraph("Фізична особа-підприємець").setFontSize(10f))
        document.add(Paragraph(fopDetails.fullName).setFontSize(10f))
        document.add(Paragraph("________________________").setFontSize(10f))
        document.add(Paragraph("(підпис)").setFontSize(8f))

        document.add(Paragraph("\n"))

        // Title
        document.add(
            Paragraph("АКТ наданих послуг")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16f)
        )
        document.add(
            Paragraph("№${invoice.number} від ${formatDate(invoice.date)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
        )

        document.add(Paragraph("\n"))

        // Introduction text
        document.add(
            Paragraph(
                "Я, що нижче підписався, Виконавець Фізична особа-підприємець ${fopDetails.fullName}, " +
                        "(реєстраційний номер облікової картки платника податків: ${fopDetails.taxId}) підтверджую, " +
                        "що були надані такі послуги:"
            ).setFontSize(10f)
        )

        document.add(Paragraph("\n"))

        // Items table
        val table = Table(floatArrayOf(1f, 6f, 2f, 2f, 2f, 3f))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Table headers
        table.addHeaderCell(createCell("№", font, true))
        table.addHeaderCell(createCell("Найменування послуг", font, true))
        table.addHeaderCell(createCell("Кількість", font, true))
        table.addHeaderCell(createCell("Одиниця", font, true))
        table.addHeaderCell(createCell("Ціна", font, true))
        table.addHeaderCell(createCell("Загальна сума", font, true))

        // Table rows
        invoice.items.forEach { item ->
            table.addCell(createCell(item.number.toString(), font, false))
            table.addCell(createCell(item.description, font, false))
            table.addCell(createCell(item.quantity.toString(), font, false))
            table.addCell(createCell(item.unit, font, false))
            table.addCell(createCell(formatCurrency(item.pricePerUnit), font, false))
            table.addCell(createCell(formatCurrency(item.totalPrice), font, false))
        }

        document.add(table)

        // Total
        document.add(
            Paragraph("Всього: ${formatCurrency(invoice.totalAmount)} грн")
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setFontSize(12f)
        )

        document.add(Paragraph("\n"))

        // Total in words
        val totalInWords = convertNumberToWords(invoice.totalAmount)
        document.add(
            Paragraph(
                "Загальна вартість послуг склала без ПДВ ${formatCurrency(invoice.totalAmount)} грн. $totalInWords"
            ).setFontSize(10f)
        )

        document.add(Paragraph("\n"))
        document.add(Paragraph("Замовник претензій по об'єму, якості та строкам надання послуг не має.").setFontSize(10f))
        document.add(Paragraph("\n"))
        document.add(Paragraph("Місце складання: місто Львів").setFontSize(10f))

        document.add(Paragraph("\n\n"))

        // Footer - Requisites
        document.add(Paragraph("Реквізити Сторін").setBold().setTextAlignment(TextAlignment.CENTER).setFontSize(12f))
        document.add(Paragraph("\n"))

        // Виконавець details
        document.add(Paragraph("Виконавець").setBold().setFontSize(11f))
        document.add(Paragraph("Фізична особа-підприємець").setFontSize(10f))
        document.add(Paragraph(fopDetails.fullName).setFontSize(10f))
        document.add(Paragraph("РНОКПП ${fopDetails.taxId}").setFontSize(10f))
        document.add(Paragraph("Адреса: ${fopDetails.address}").setFontSize(10f))
        document.add(Paragraph("\nБанківські деталі").setBold().setFontSize(10f))
        document.add(Paragraph("Номер рахунку (IBAN): ${fopDetails.iban}").setFontSize(9f))
        document.add(Paragraph("Банк: ${fopDetails.bank}").setFontSize(10f))
        document.add(Paragraph("МФО: ${fopDetails.mfo}").setFontSize(10f))
        document.add(Paragraph("\nКонтактні деталі").setBold().setFontSize(10f))
        document.add(Paragraph("тел.: ${fopDetails.phone}").setFontSize(10f))
        document.add(Paragraph("email: ${fopDetails.email}").setFontSize(10f))

        document.add(Paragraph("\n\n"))
        document.add(Paragraph("${fopDetails.fullName} _______________________").setFontSize(10f))
        document.add(Paragraph("(підпис)").setFontSize(8f))

        // Close document
        document.close()

        return filePath
    }

    private fun createCell(text: String, font: PdfFont, isHeader: Boolean): Cell {
        val cell = Cell().add(Paragraph(text).setFont(font).setFontSize(9f))
        if (isHeader) {
            cell.setBold()
        }
        return cell
    }

    private fun formatDate(instant: kotlin.time.Instant): String {
        // Convert kotlin.time.Instant to kotlinx.datetime.Instant for formatting
        val kotlinxInstant = Instant.fromEpochMilliseconds(instant.toEpochMilliseconds())
        val localDateTime = kotlinxInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.dayOfMonth.toString().padStart(2, '0')}.${
            localDateTime.monthNumber.toString().padStart(2, '0')
        }.${localDateTime.year}"
    }

    private fun formatCurrency(amount: Double): String {
        return String.format("%.2f", amount)
    }

    private fun convertNumberToWords(amount: Double): String {
        // Basic implementation for Ukrainian number to words conversion
        // This is a simplified version - in production, you'd want a complete implementation
        val intPart = amount.toInt()
        val decPart = ((amount - intPart) * 100).toInt()

        val thousands = intPart / 1000
        val hundreds = (intPart % 1000) / 100
        val tens = (intPart % 100) / 10
        val ones = intPart % 10

        val result = StringBuilder()

        // Thousands
        if (thousands > 0) {
            result.append(digitToWords(thousands))
            result.append(" ")
            result.append(getThousandsWord(thousands))
            result.append(" ")
        }

        // Hundreds
        if (hundreds > 0) {
            result.append(hundredsToWords(hundreds))
            result.append(" ")
        }

        // Tens and ones
        if (tens == 1) {
            result.append(teensToWords(ones))
        } else {
            if (tens > 0) {
                result.append(tensToWords(tens))
                result.append(" ")
            }
            if (ones > 0 || intPart == 0) {
                result.append(digitToWords(ones))
            }
        }

        result.append(" ")
        result.append(getHryvniaWord(intPart))
        result.append(" ")
        result.append(String.format("%02d", decPart))
        result.append(" ")
        result.append(getKopiykaWord(decPart))

        return result.toString().trim()
    }

    private fun digitToWords(digit: Int): String = when (digit) {
        0 -> "нуль"
        1 -> "один"
        2 -> "два"
        3 -> "три"
        4 -> "чотири"
        5 -> "п'ять"
        6 -> "шість"
        7 -> "сім"
        8 -> "вісім"
        9 -> "дев'ять"
        else -> ""
    }

    private fun teensToWords(digit: Int): String = when (digit) {
        0 -> "десять"
        1 -> "одинадцять"
        2 -> "дванадцять"
        3 -> "тринадцять"
        4 -> "чотирнадцять"
        5 -> "п'ятнадцять"
        6 -> "шістнадцять"
        7 -> "сімнадцять"
        8 -> "вісімнадцять"
        9 -> "дев'ятнадцять"
        else -> ""
    }

    private fun tensToWords(tens: Int): String = when (tens) {
        2 -> "двадцять"
        3 -> "тридцять"
        4 -> "сорок"
        5 -> "п'ятдесят"
        6 -> "шістдесят"
        7 -> "сімдесят"
        8 -> "вісімдесят"
        9 -> "дев'яносто"
        else -> ""
    }

    private fun hundredsToWords(hundreds: Int): String = when (hundreds) {
        1 -> "сто"
        2 -> "двісті"
        3 -> "триста"
        4 -> "чотириста"
        5 -> "п'ятсот"
        6 -> "шістсот"
        7 -> "сімсот"
        8 -> "вісімсот"
        9 -> "дев'ятсот"
        else -> ""
    }

    private fun getThousandsWord(thousands: Int): String {
        val lastDigit = thousands % 10
        val lastTwoDigits = thousands % 100

        return when {
            lastTwoDigits in 11..14 -> "тисяч"
            lastDigit == 1 -> "тисяча"
            lastDigit in 2..4 -> "тисячі"
            else -> "тисяч"
        }
    }

    private fun getHryvniaWord(amount: Int): String {
        val lastDigit = amount % 10
        val lastTwoDigits = amount % 100

        return when {
            lastTwoDigits in 11..14 -> "гривень"
            lastDigit == 1 -> "гривня"
            lastDigit in 2..4 -> "гривні"
            else -> "гривень"
        }
    }

    private fun getKopiykaWord(amount: Int): String {
        val lastDigit = amount % 10
        val lastTwoDigits = amount % 100

        return when {
            lastTwoDigits in 11..14 -> "копійок"
            lastDigit == 1 -> "копійка"
            lastDigit in 2..4 -> "копійки"
            else -> "копійок"
        }
    }
}
