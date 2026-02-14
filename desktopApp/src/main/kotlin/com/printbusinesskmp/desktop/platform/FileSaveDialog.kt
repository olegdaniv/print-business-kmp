package com.printbusinesskmp.desktop.platform

import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun choosePdfSavePath(defaultFileName: String): Path? {
    val chooser = JFileChooser(DesktopPaths.invoiceDownloadsDir.toFile()).apply {
        dialogTitle = "Save invoice PDF"
        fileFilter = FileNameExtensionFilter("PDF files", "pdf")
        selectedFile = DesktopPaths.invoiceDownloadsDir.resolve(defaultFileName).toFile()
    }

    val result = chooser.showSaveDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) {
        return null
    }

    val selected = chooser.selectedFile.toPath()
    return if (selected.fileName.toString().lowercase().endsWith(".pdf")) {
        selected
    } else {
        selected.resolveSibling("${selected.fileName}.pdf")
    }
}
