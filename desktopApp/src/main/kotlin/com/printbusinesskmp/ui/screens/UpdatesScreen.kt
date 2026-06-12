package com.printbusinesskmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.desktop.update.UpdateUiState
import com.printbusinesskmp.theme.AppColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UpdatesScreen(
    state: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismissError: () -> Unit
) {
    var showInstallDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Оновлення",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.DarkSlate
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledValue("Поточна версія", state.currentVersion)
                LabeledValue(
                    "Остання перевірка",
                    state.lastCheckedAt?.let(::formatTimestamp) ?: "Ще не перевірялося"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !state.isChecking && !state.isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                    ) {
                        Text("Перевірити оновлення", color = AppColors.White)
                    }

                    if (state.isChecking) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }

                if (state.latestVersion != null) {
                    val statusText = if (state.updateAvailable) {
                        "Доступна нова версія: ${state.latestVersion}"
                    } else {
                        "Ви використовуєте актуальну версію (${state.latestVersion})"
                    }
                    Text(
                        text = statusText,
                        color = AppColors.DarkSlate,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (state.releaseNotes.isNotBlank()) {
                    Text(
                        text = "Що нового",
                        color = AppColors.DarkGrayText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = state.releaseNotes, color = AppColors.MediumGray)
                }

                if (state.updateAvailable && state.downloadedInstaller == null && !state.isDownloading) {
                    Button(
                        onClick = onDownloadUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Success)
                    ) {
                        Text("Оновити", color = AppColors.White)
                    }
                }

                if (state.isDownloading) {
                    val progress = state.progressFraction
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Text(
                        text = buildProgressText(state),
                        color = AppColors.MediumGray,
                        fontSize = 13.sp
                    )

                    TextButton(onClick = onCancelDownload) {
                        Text("Скасувати завантаження")
                    }
                }

                if (state.downloadedInstaller != null) {
                    Text(
                        text = "Оновлення завантажено: ${state.downloadedInstaller.fileName}",
                        color = AppColors.Success
                    )
                    Button(
                        onClick = { showInstallDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                    ) {
                        Text("Встановити оновлення", color = AppColors.White)
                    }
                }
            }
        }

        if (state.warningMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppColors.CardItemBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.warningMessage,
                    color = AppColors.DarkGrayText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (state.errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = state.errorMessage, color = AppColors.Error)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismissError) {
                            Text("Закрити")
                        }
                        TextButton(onClick = onCheckForUpdates) {
                            Text("Повторити перевірку")
                        }
                    }
                }
            }
        }
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Встановити оновлення") },
            text = { Text("Програму буде закрито та запущено MSI-інсталятор. Продовжити?") },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallDialog = false
                        onInstallUpdate()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue)
                ) {
                    Text("Встановити", color = AppColors.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = AppColors.MediumGray,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = AppColors.DarkSlate,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

private fun buildProgressText(state: UpdateUiState): String {
    val downloaded = formatMegaBytes(state.downloadedBytes)
    val totalBytes = state.totalBytes
    val total = totalBytes?.let(::formatMegaBytes)

    return if (total != null && totalBytes > 0L) {
        val percent = ((state.downloadedBytes * 100.0) / totalBytes).coerceIn(0.0, 100.0)
        "${"%.1f".format(Locale.US, percent)}% ($downloaded / $total)"
    } else {
        "$downloaded завантажено"
    }
}

private fun formatMegaBytes(bytes: Long): String {
    val megabytes = bytes / (1024.0 * 1024.0)
    return "${"%.2f".format(Locale.US, megabytes)} MB"
}
