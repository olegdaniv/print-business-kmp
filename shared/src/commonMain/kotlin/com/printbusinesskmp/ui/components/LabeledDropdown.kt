package com.printbusinesskmp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.theme.AppColors

/**
 * A clearly tappable dropdown selector: a small caption label above an outlined,
 * accent-bordered button that shows the current value plus a ▾ arrow.
 *
 * Replaces the low-contrast "Label: value  [Змінити]" plain-text-button pattern,
 * which blended into the surrounding text and read as static content rather than
 * an interactive control.
 */
@Composable
fun <T> LabeledDropdown(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.MediumGray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, AppColors.PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.DarkSlate)
            ) {
                Text(selectedText, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                Text("▾", color = AppColors.PrimaryBlue, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
