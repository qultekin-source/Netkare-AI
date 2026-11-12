package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object StudioButtonStyles {
    @Composable
    fun primaryColors(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary
    ) = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    @Composable
    fun secondaryFilledColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    )

    @Composable
    fun aiAccentColors() = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.secondary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    )

    @Composable
    fun successColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    @Composable
    fun prominentElevation() = ButtonDefaults.buttonElevation(
        defaultElevation = 6.dp,
        pressedElevation = 2.dp,
        disabledElevation = 0.dp
    )
}

@Composable
fun ProminentPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = MaterialTheme.shapes.large,
        elevation = StudioButtonStyles.prominentElevation(),
        colors = StudioButtonStyles.primaryColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (enabled) {
            BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))
        } else {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        },
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
fun ProminentSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 50.dp,
    accentBorder: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = MaterialTheme.shapes.medium,
        colors = if (accentBorder) {
            StudioButtonStyles.aiAccentColors()
        } else {
            StudioButtonStyles.secondaryFilledColors()
        },
        border = BorderStroke(
            width = 1.5.dp,
            color = when {
                !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                accentBorder -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline
            }
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
fun ProminentSuccessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 50.dp,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = MaterialTheme.shapes.medium,
        elevation = StudioButtonStyles.prominentElevation(),
        colors = StudioButtonStyles.successColors(),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        content = content
    )
}
