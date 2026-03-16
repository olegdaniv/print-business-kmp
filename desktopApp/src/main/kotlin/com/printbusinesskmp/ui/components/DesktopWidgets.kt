package com.printbusinesskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printbusinesskmp.models.OrderStatus
import com.printbusinesskmp.models.PaymentStatus
import com.printbusinesskmp.ui.theme.DesktopColors
import com.printbusinesskmp.utils.labelUa

@Composable
fun StatusBadge(status: OrderStatus) {
    val (bg, text) = when (status) {
        OrderStatus.DRAFT, @Suppress("DEPRECATION") OrderStatus.NEW ->
            DesktopColors.Status.draftBg to DesktopColors.Status.draftText
        OrderStatus.PENDING_APPROVAL ->
            DesktopColors.Status.pendingBg to DesktopColors.Status.pendingText
        OrderStatus.APPROVED ->
            DesktopColors.Status.approvedBg to DesktopColors.Status.approvedText
        OrderStatus.OUTSOURCE_ORDERED, OrderStatus.OUTSOURCE_RECEIVED ->
            DesktopColors.Status.inProgressBg to DesktopColors.Status.inProgressText
        OrderStatus.IN_PRODUCTION, OrderStatus.QUALITY_CHECK ->
            DesktopColors.Status.inProgressBg to DesktopColors.Status.inProgressText
        OrderStatus.READY ->
            DesktopColors.Status.readyBg to DesktopColors.Status.readyText
        OrderStatus.SHIPPED ->
            DesktopColors.Status.shippedBg to DesktopColors.Status.shippedText
        OrderStatus.COMPLETED ->
            DesktopColors.Status.completedBg to DesktopColors.Status.completedText
        OrderStatus.CANCELLED ->
            DesktopColors.Status.cancelledBg to DesktopColors.Status.cancelledText
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.labelUa(),
            color = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PaymentBadge(status: PaymentStatus) {
    val (bg, text) = when (status) {
        PaymentStatus.UNPAID -> DesktopColors.Status.cancelledBg to DesktopColors.Status.cancelledText
        PaymentStatus.PARTIAL -> DesktopColors.Status.pendingBg to DesktopColors.Status.pendingText
        PaymentStatus.PAID -> DesktopColors.Status.completedBg to DesktopColors.Status.completedText
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.labelUa(),
            color = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Пошук...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.height(40.dp)
    )
}

@Composable
fun <T> StatusFilterChips(
    allLabel: String = "Всі",
    values: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    labelMapper: (T) -> String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(allLabel, fontSize = 12.sp) },
            shape = RoundedCornerShape(6.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(labelMapper(value), fontSize = 12.sp) },
                shape = RoundedCornerShape(6.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun HoverableRow(
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(bgColor)
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        trailing?.invoke()
    }
}

