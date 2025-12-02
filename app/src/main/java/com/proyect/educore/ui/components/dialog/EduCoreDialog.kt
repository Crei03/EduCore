package com.proyect.educore.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.Error
import com.proyect.educore.ui.theme.Success
import com.proyect.educore.ui.theme.Warning

/**
 * Tipos de diálogo disponibles
 */
enum class DialogType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    CONFIRMATION
}

/**
 * Diálogo personalizado para EduCore con diseño moderno y minimalista
 *
 * @param visible Si el diálogo está visible
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param title Título del diálogo
 * @param message Mensaje principal
 * @param type Tipo de diálogo para determinar el estilo visual
 * @param confirmText Texto del botón de confirmación
 * @param cancelText Texto del botón de cancelar (null para ocultarlo)
 * @param onConfirm Callback cuando se confirma
 * @param onCancel Callback cuando se cancela
 * @param isLoading Si se debe mostrar estado de carga
 * @param dismissOnBackPress Si se puede cerrar con back
 * @param dismissOnClickOutside Si se puede cerrar tocando fuera
 */
@Composable
fun EduCoreDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    type: DialogType = DialogType.INFO,
    confirmText: String = "Aceptar",
    cancelText: String? = null,
    onConfirm: () -> Unit = onDismiss,
    onCancel: () -> Unit = onDismiss,
    isLoading: Boolean = false,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable (() -> Unit)? = null
) {
    if (visible) {
        Dialog(
            onDismissRequest = {
                if (!isLoading && dismissOnClickOutside) onDismiss()
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress && !isLoading,
                dismissOnClickOutside = dismissOnClickOutside && !isLoading,
                usePlatformDefaultWidth = false
            )
        ) {
            EduCoreDialogContent(
                title = title,
                message = message,
                type = type,
                confirmText = confirmText,
                cancelText = cancelText,
                onConfirm = onConfirm,
                onCancel = onCancel,
                isLoading = isLoading,
                content = content
            )
        }
    }
}

@Composable
private fun EduCoreDialogContent(
    title: String,
    message: String,
    type: DialogType,
    confirmText: String,
    cancelText: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean,
    content: @Composable (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme

    val (accentColor, iconSpec) = when (type) {
        DialogType.SUCCESS -> Success to RemoteIconSpec.Check
        DialogType.ERROR -> Error to RemoteIconSpec.Error
        DialogType.WARNING -> Warning to RemoteIconSpec.Warning
        DialogType.CONFIRMATION -> colorScheme.primary to RemoteIconSpec.Help
        DialogType.INFO -> colorScheme.primary to RemoteIconSpec.Info
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono con fondo circular
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                RemoteIcon(
                    iconSpec = iconSpec,
                    tint = accentColor,
                    size = 32.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Título
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mensaje
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Contenido personalizado
            content?.let {
                Spacer(modifier = Modifier.height(16.dp))
                it()
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (cancelText != null) {
                    Arrangement.spacedBy(12.dp)
                } else {
                    Arrangement.Center
                }
            ) {
                // Botón cancelar
                cancelText?.let {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Botón confirmar
                Button(
                    onClick = onConfirm,
                    modifier = if (cancelText != null) {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                    } else {
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (type) {
                            DialogType.ERROR -> Error
                            DialogType.WARNING -> Warning
                            else -> colorScheme.primary
                        }
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = confirmText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Versión simplificada del diálogo para confirmaciones rápidas
 */
@Composable
fun EduCoreConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    confirmText: String = "Confirmar",
    cancelText: String = "Cancelar",
    isDestructive: Boolean = false,
    isLoading: Boolean = false
) {
    EduCoreDialog(
        visible = visible,
        onDismiss = onDismiss,
        title = title,
        message = message,
        type = if (isDestructive) DialogType.ERROR else DialogType.CONFIRMATION,
        confirmText = confirmText,
        cancelText = cancelText,
        onConfirm = onConfirm,
        onCancel = onDismiss,
        isLoading = isLoading
    )
}

/**
 * Diálogo de alerta simple (solo botón aceptar)
 */
@Composable
fun EduCoreAlertDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String,
    type: DialogType = DialogType.INFO,
    buttonText: String = "Entendido"
) {
    EduCoreDialog(
        visible = visible,
        onDismiss = onDismiss,
        title = title,
        message = message,
        type = type,
        confirmText = buttonText,
        cancelText = null,
        onConfirm = onDismiss
    )
}
