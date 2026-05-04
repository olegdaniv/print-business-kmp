package com.printbusinesskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
fun SplitPane(
    modifier: Modifier = Modifier,
    initialRatio: Float = 0.35f,
    minLeftFraction: Float = 0.2f,
    maxLeftFraction: Float = 0.5f,
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit
) {
    var splitRatio by remember { mutableStateOf(initialRatio) }
    var totalWidth by remember { mutableStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { totalWidth = it.width }
    ) {
        // Left panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(splitRatio)
        ) {
            leftContent()
        }

        // Draggable divider
        val dividerColor = if (isHovered) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(dividerColor)
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.W_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        if (totalWidth > 0) {
                            val delta = dragAmount.x / totalWidth
                            splitRatio = (splitRatio + delta).coerceIn(minLeftFraction, maxLeftFraction)
                        }
                    }
                }
        )

        // Right panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f - splitRatio)
        ) {
            rightContent()
        }
    }
}
