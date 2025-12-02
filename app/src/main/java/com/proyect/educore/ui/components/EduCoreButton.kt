package com.proyect.educore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Variantes de botón disponibles
 */
enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    OUTLINE,
    TEXT,
    DESTRUCTIVE
}

/**
 * Tamaños de botón
 */
enum class ButtonSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Botón personalizado para EduCore con diseño moderno
 *
 * @param text Texto del botón
 * @param onClick Callback al hacer click
 * @param modifier Modificador
 * @param variant Variante de estilo
 * @param size Tamaño del botón
 * @param enabled Si está habilitado
 * @param isLoading Si muestra estado de carga
 * @param leadingIcon Icono opcional al inicio
 * @param fullWidth Si ocupa todo el ancho
 */
@Composable
fun EduCoreButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: RemoteIconSpec? = null,
    fullWidth: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val height = when (size) {
        ButtonSize.SMALL -> 36.dp
        ButtonSize.MEDIUM -> 48.dp
        ButtonSize.LARGE -> 56.dp
    }

    val horizontalPadding = when (size) {
        ButtonSize.SMALL -> 16.dp
        ButtonSize.MEDIUM -> 24.dp
        ButtonSize.LARGE -> 32.dp
    }

    val textStyle = when (size) {
        ButtonSize.SMALL -> MaterialTheme.typography.labelMedium
        ButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
        ButtonSize.LARGE -> MaterialTheme.typography.titleMedium
    }

    val iconSize = when (size) {
        ButtonSize.SMALL -> 16.dp
        ButtonSize.MEDIUM -> 20.dp
        ButtonSize.LARGE -> 24.dp
    }

    val (containerColor, contentColor, borderColor) = when (variant) {
        ButtonVariant.PRIMARY -> Triple(
            colorScheme.primary,
            colorScheme.onPrimary,
            null
        )
        ButtonVariant.SECONDARY -> Triple(
            colorScheme.secondaryContainer,
            colorScheme.onSecondaryContainer,
            null
        )
        ButtonVariant.TERTIARY -> Triple(
            colorScheme.tertiaryContainer,
            colorScheme.onTertiaryContainer,
            null
        )
        ButtonVariant.OUTLINE -> Triple(
            Color.Transparent,
            colorScheme.primary,
            colorScheme.primary
        )
        ButtonVariant.TEXT -> Triple(
            Color.Transparent,
            colorScheme.primary,
            null
        )
        ButtonVariant.DESTRUCTIVE -> Triple(
            colorScheme.error,
            colorScheme.onError,
            null
        )
    }

    val elevation by animateDpAsState(
        targetValue = when {
            !enabled -> 0.dp
            isPressed -> 1.dp
            variant == ButtonVariant.PRIMARY -> 4.dp
            variant == ButtonVariant.SECONDARY -> 2.dp
            else -> 0.dp
        },
        animationSpec = tween(150),
        label = "elevation"
    )

    val shape = RoundedCornerShape(14.dp)

    val baseModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .height(height)
        .shadow(elevation, shape, spotColor = containerColor.copy(alpha = 0.3f))

    when (variant) {
        ButtonVariant.OUTLINE -> {
            OutlinedButton(
                onClick = { if (!isLoading) onClick() },
                modifier = baseModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(horizontal = horizontalPadding)
            ) {
                ButtonContent(
                    text = text,
                    loading = isLoading,
                    iconSpec = leadingIcon,
                    iconSize = iconSize,
                    textStyle = textStyle,
                    contentColor = contentColor
                )
            }
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = { if (!isLoading) onClick() },
                modifier = baseModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(horizontal = horizontalPadding)
            ) {
                ButtonContent(
                    text = text,
                    loading = isLoading,
                    iconSpec = leadingIcon,
                    iconSize = iconSize,
                    textStyle = textStyle,
                    contentColor = contentColor
                )
            }
        }
        else -> {
            Button(
                onClick = { if (!isLoading) onClick() },
                modifier = baseModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                    disabledContainerColor = containerColor.copy(alpha = 0.38f),
                    disabledContentColor = contentColor.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = horizontalPadding)
            ) {
                ButtonContent(
                    text = text,
                    loading = isLoading,
                    iconSpec = leadingIcon,
                    iconSize = iconSize,
                    textStyle = textStyle,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    iconSpec: RemoteIconSpec?,
    iconSize: Dp,
    textStyle: androidx.compose.ui.text.TextStyle,
    contentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            iconSpec?.let {
                RemoteIcon(
                    iconSpec = it,
                    tint = contentColor,
                    size = iconSize
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Text(
            text = text,
            style = textStyle,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Botón de icono circular
 */
@Composable
fun EduCoreIconButton(
    iconSpec: RemoteIconSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM,
    variant: ButtonVariant = ButtonVariant.SECONDARY
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonSize = when (size) {
        ButtonSize.SMALL -> 36.dp
        ButtonSize.MEDIUM -> 44.dp
        ButtonSize.LARGE -> 56.dp
    }

    val iconSize = when (size) {
        ButtonSize.SMALL -> 18.dp
        ButtonSize.MEDIUM -> 22.dp
        ButtonSize.LARGE -> 28.dp
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> colorScheme.onSurface.copy(alpha = 0.12f)
            isPressed -> when (variant) {
                ButtonVariant.PRIMARY -> colorScheme.primary.copy(alpha = 0.8f)
                else -> colorScheme.surfaceVariant
            }
            else -> when (variant) {
                ButtonVariant.PRIMARY -> colorScheme.primary
                ButtonVariant.SECONDARY -> colorScheme.secondaryContainer
                ButtonVariant.TERTIARY -> colorScheme.tertiaryContainer
                else -> colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        },
        animationSpec = tween(150),
        label = "backgroundColor"
    )

    val iconColor = when (variant) {
        ButtonVariant.PRIMARY -> colorScheme.onPrimary
        ButtonVariant.SECONDARY -> colorScheme.onSecondaryContainer
        ButtonVariant.TERTIARY -> colorScheme.onTertiaryContainer
        else -> colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        RemoteIcon(
            iconSpec = iconSpec,
            tint = if (enabled) iconColor else colorScheme.onSurface.copy(alpha = 0.38f),
            size = iconSize
        )
    }
}
