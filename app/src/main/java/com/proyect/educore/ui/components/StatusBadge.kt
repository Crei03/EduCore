package com.proyect.educore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.educore.ui.theme.Error
import com.proyect.educore.ui.theme.Success
import com.proyect.educore.ui.theme.Warning

/**
 * Tipos de estado para badges
 */
enum class StatusType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    NEUTRAL,
    ACTIVE,
    INACTIVE,
    PENDING
}

/**
 * Badge de estado personalizado para EduCore
 *
 * @param text Texto del badge
 * @param status Tipo de estado
 * @param modifier Modificador
 * @param showDot Si muestra un punto de color
 */
@Composable
fun EduCoreStatusBadge(
    text: String,
    status: StatusType,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme

    val (backgroundColor, contentColor) = when (status) {
        StatusType.SUCCESS -> Pair(
            Success.copy(alpha = 0.12f),
            Success
        )
        StatusType.WARNING -> Pair(
            Warning.copy(alpha = 0.12f),
            Warning
        )
        StatusType.ERROR -> Pair(
            Error.copy(alpha = 0.12f),
            Error
        )
        StatusType.INFO -> Pair(
            colorScheme.primary.copy(alpha = 0.12f),
            colorScheme.primary
        )
        StatusType.NEUTRAL -> Pair(
            colorScheme.surfaceVariant,
            colorScheme.onSurfaceVariant
        )
        StatusType.ACTIVE -> Pair(
            colorScheme.secondary.copy(alpha = 0.12f),
            colorScheme.secondary
        )
        StatusType.INACTIVE -> Pair(
            colorScheme.outline.copy(alpha = 0.12f),
            colorScheme.outline
        )
        StatusType.PENDING -> Pair(
            colorScheme.tertiary.copy(alpha = 0.12f),
            colorScheme.tertiary
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(contentColor)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * Badge pequeño solo con punto de color
 */
@Composable
fun EduCoreDotBadge(
    status: StatusType,
    modifier: Modifier = Modifier,
    size: Int = 8
) {
    val color = when (status) {
        StatusType.SUCCESS -> Success
        StatusType.WARNING -> Warning
        StatusType.ERROR -> Error
        StatusType.INFO -> MaterialTheme.colorScheme.primary
        StatusType.ACTIVE -> MaterialTheme.colorScheme.secondary
        StatusType.INACTIVE -> MaterialTheme.colorScheme.outline
        StatusType.PENDING -> MaterialTheme.colorScheme.tertiary
        StatusType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Badge con contador numérico
 */
@Composable
fun EduCoreCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    status: StatusType = StatusType.ERROR
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val backgroundColor = when (status) {
        StatusType.SUCCESS -> Success
        StatusType.WARNING -> Warning
        StatusType.ERROR -> Error
        StatusType.INFO -> colorScheme.primary
        else -> colorScheme.primary
    }

    val displayText = if (count > maxCount) "$maxCount+" else count.toString()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Chip de filtro seleccionable
 */
@Composable
fun EduCoreFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
