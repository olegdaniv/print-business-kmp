package com.printbusinesskmp.desktop.platform

import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun chooseSavePath(
    defaultFileName: String,
    dialogTitle: String,
    extensions: List<String> = emptyList(),
    description: String? = null
): Path? {
    val chooser = JFileChooser(DesktopPaths.invoiceDownloadsDir.toFile()).apply {
        this.dialogTitle = dialogTitle
        selectedFile = DesktopPaths.invoiceDownloadsDir.resolve(defaultFileName).toFile()
        if (extensions.isNotEmpty()) {
            fileFilter = FileNameExtensionFilter(description ?: "Files", *extensions.toTypedArray())
        }
    }

    val result = chooser.showSaveDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) {
        return null
    }

    val selected = chooser.selectedFile.toPath()
    if (extensions.isEmpty()) {
        return selected
    }

    val lowerFileName = selected.fileName.toString().lowercase()
    val hasExtension = extensions.any { extension -> lowerFileName.endsWith(".${extension.lowercase()}") }

    return if (hasExtension) {
        selected
    } else {
        selected.resolveSibling("${selected.fileName}.${extensions.first()}")
    }
}

fun choosePdfSavePath(defaultFileName: String): Path? {
    return chooseSavePath(
        defaultFileName = defaultFileName,
        dialogTitle = "Save invoice PDF",
        extensions = listOf("pdf"),
        description = "PDF files"
    )
}

fun chooseDirectory(
    dialogTitle: String,
    start: Path = AppSettingsStore.invoicesDir
): Path? {
    val chooser = JFileChooser(start.toFile()).apply {
        this.dialogTitle = dialogTitle
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
}

fun chooseImageFilePath(): Path? {
    val chooser = JFileChooser(DesktopPaths.invoiceDownloadsDir.toFile()).apply {
        dialogTitle = "Choose preview image"
        fileFilter = FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "webp", "gif", "svg")
    }
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
}
