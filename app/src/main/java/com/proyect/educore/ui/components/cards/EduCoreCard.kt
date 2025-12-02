package com.proyect.educore.ui.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec

/**
 * Variantes de estilo para EduCoreCard
 */
enum class CardVariant {
    ELEVATED,      // Con sombra y elevación
    FILLED,        // Con fondo sólido
    OUTLINED,      // Con borde
    GLASS,         // Efecto glassmorphism
    GRADIENT       // Con gradiente sutil
}

/**
 * Card personalizada para EduCore con diseño moderno
 *
 * @param modifier Modificador
 * @param variant Variante de estilo
 * @param onClick Callback al hacer click (null para no clickeable)
 * @param content Contenido de la card
 */
@Composable
fun EduCoreCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.ELEVATED,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        targetValue = when {
            isPressed && variant == CardVariant.ELEVATED -> 2.dp
            variant == CardVariant.ELEVATED -> 8.dp
            else -> 0.dp
        },
        animationSpec = tween(150),
        label = "elevation"
    )

    val backgroundColor = when (variant) {
        CardVariant.ELEVATED -> colorScheme.surface
        CardVariant.FILLED -> colorScheme.surfaceVariant.copy(alpha = 0.7f)
        CardVariant.OUTLINED -> colorScheme.surface
        CardVariant.GLASS -> colorScheme.surface.copy(alpha = 0.8f)
        CardVariant.GRADIENT -> colorScheme.surface
    }

    val border = when (variant) {
        CardVariant.OUTLINED -> BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
        CardVariant.GLASS -> BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))
        else -> null
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(20.dp),
                spotColor = colorScheme.primary.copy(alpha = 0.08f)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = border
    ) {
        if (variant == CardVariant.GRADIENT) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colorScheme.primary.copy(alpha = 0.03f),
                                colorScheme.surface
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    content = content
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

/**
 * Card destacada con icono y título para acciones principales
 */
@Composable
fun EduCoreFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    iconSpec: RemoteIconSpec,
    onClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val colorScheme = MaterialTheme.colorScheme

    EduCoreCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.GRADIENT,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono con fondo circular
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                RemoteIcon(
                    iconSpec = iconSpec,
                    tint = accentColor,
                    size = 28.dp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            RemoteIcon(
                iconSpec = RemoteIconSpec.ArrowForward,
                tint = accentColor.copy(alpha = 0.6f),
                size = 20.dp
            )
        }
    }
}

/**
 * Card de estadística con número destacado
 */
@Composable
fun EduCoreStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    iconSpec: RemoteIconSpec? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val colorScheme = MaterialTheme.colorScheme

    EduCoreCard(
        modifier = modifier,
        variant = CardVariant.FILLED
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconSpec?.let {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    RemoteIcon(
                        iconSpec = it,
                        tint = accentColor,
                        size = 24.dp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Card de información con lista de detalles
 */
@Composable
fun EduCoreInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    items: List<Pair<String, String>>,
    variant: CardVariant = CardVariant.OUTLINED
) {
    val colorScheme = MaterialTheme.colorScheme

    EduCoreCard(
        modifier = modifier.fillMaxWidth(),
        variant = variant
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        items.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Card vacía con mensaje de estado
 */
@Composable
fun EduCoreEmptyCard(
    modifier: Modifier = Modifier,
    iconSpec: RemoteIconSpec = RemoteIconSpec.List,
    title: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme

    EduCoreCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.FILLED
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colorScheme.outline.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                RemoteIcon(
                    iconSpec = iconSpec,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    size = 32.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
