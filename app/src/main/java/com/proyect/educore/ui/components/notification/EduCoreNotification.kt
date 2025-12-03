package com.proyect.educore.ui.components.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.Error
import com.proyect.educore.ui.theme.Success
import com.proyect.educore.ui.theme.Warning
import kotlinx.coroutines.delay

/**
 * Tipos de notificación disponibles para EduCore
 */
enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Estado de una notificación
 */
@Stable
data class NotificationData(
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val durationMs: Long = 3000L,
    val id: Long = System.currentTimeMillis()
)

/**
 * Estado para manejar notificaciones en la UI
 */
@Stable
class NotificationState {
    var currentNotification by mutableStateOf<NotificationData?>(null)
        private set

    fun show(message: String, type: NotificationType = NotificationType.INFO, durationMs: Long = 3000L) {
        currentNotification = NotificationData(
            message = message,
            type = type,
            durationMs = durationMs
        )
    }

    fun showSuccess(message: String, durationMs: Long = 3000L) {
        show(message, NotificationType.SUCCESS, durationMs)
    }

    fun showError(message: String, durationMs: Long = 4000L) {
        show(message, NotificationType.ERROR, durationMs)
    }

    fun showWarning(message: String, durationMs: Long = 3500L) {
        show(message, NotificationType.WARNING, durationMs)
    }

    fun showInfo(message: String, durationMs: Long = 3000L) {
        show(message, NotificationType.INFO, durationMs)
    }

    fun dismiss() {
        currentNotification = null
    }
}

@Composable
fun rememberNotificationState(): NotificationState {
    return remember { NotificationState() }
}

/**
 * Composable para mostrar notificaciones personalizadas tipo toast
 * Debe colocarse en la raíz del Scaffold o contenedor principal
 */
@Composable
fun EduCoreNotificationHost(
    state: NotificationState,
    modifier: Modifier = Modifier
) {
    val notification = state.currentNotification

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            notification?.let { data ->
                EduCoreNotificationItem(
                    data = data,
                    onDismiss = { state.dismiss() }
                )
            }
        }
    }

    // Auto-dismiss después de la duración especificada
    LaunchedEffect(notification?.id) {
        notification?.let {
            delay(it.durationMs)
            state.dismiss()
        }
    }
}

@Composable
private fun EduCoreNotificationItem(
    data: NotificationData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f

    val (backgroundColor, contentColor, iconSpec) = when (data.type) {
        NotificationType.SUCCESS -> Triple(
            if (isDarkTheme) Success.copy(alpha = 0.9f) else Success,
            Color.White,
            RemoteIconSpec.Check // Representando éxito
        )
        NotificationType.ERROR -> Triple(
            if (isDarkTheme) Error.copy(alpha = 0.9f) else Error,
            Color.White,
            RemoteIconSpec.Error // Representando error/x
        )
        NotificationType.WARNING -> Triple(
            if (isDarkTheme) Warning.copy(alpha = 0.9f) else Warning,
            if (isDarkTheme) Color.White else Color.Black,
            RemoteIconSpec.Warning // Representando advertencia
        )
        NotificationType.INFO -> Triple(
            if (isDarkTheme) colorScheme.primaryContainer else colorScheme.primary,
            if (isDarkTheme) colorScheme.onPrimaryContainer else colorScheme.onPrimary,
            RemoteIconSpec.Info // Representando info
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = backgroundColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIcon(
                iconSpec = iconSpec,
                tint = contentColor,
                size = 24.dp
            )
            Text(
                text = data.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Helper extension para calcular luminancia
 */
private fun Color.luminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
