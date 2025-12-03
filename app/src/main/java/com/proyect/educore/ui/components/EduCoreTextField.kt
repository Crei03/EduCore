package com.proyect.educore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Campo de texto personalizado para EduCore con diseño moderno
 *
 * @param value Valor actual
 * @param onValueChange Callback cuando cambia el valor
 * @param modifier Modificador
 * @param label Etiqueta del campo
 * @param placeholder Texto placeholder
 * @param leadingIcon Icono al inicio
 * @param trailingIcon Icono al final
 * @param isPassword Si es campo de contraseña
 * @param isError Si tiene error
 * @param errorMessage Mensaje de error
 * @param enabled Si está habilitado
 * @param singleLine Si es una sola línea
 * @param keyboardType Tipo de teclado
 * @param imeAction Acción del teclado
 * @param onImeAction Callback cuando se ejecuta la acción del teclado
 */
@Composable
fun EduCoreTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: RemoteIconSpec? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colorScheme.error
            isFocused -> colorScheme.primary
            else -> colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "borderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || isError) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "borderWidth"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> colorScheme.surfaceVariant.copy(alpha = 0.5f)
            isFocused -> colorScheme.surface
            else -> colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    Column(modifier = modifier) {
        // Label
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isError -> colorScheme.error
                    isFocused -> colorScheme.primary
                    else -> colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }

        // Campo de texto
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(backgroundColor)
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
                onSearch = { onImeAction() },
                onSend = { onImeAction() },
                onGo = { onImeAction() }
            ),
            singleLine = singleLine,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leading icon
                    leadingIcon?.let {
                        RemoteIcon(
                            iconSpec = it,
                            tint = when {
                                isError -> colorScheme.error
                                isFocused -> colorScheme.primary
                                else -> colorScheme.onSurfaceVariant
                            },
                            size = 22.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Text field content
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }

                    // Trailing icon o toggle de contraseña
                    if (isPassword) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            RemoteIcon(
                                iconSpec = if (passwordVisible) {
                                    RemoteIconSpec.VisibilityOff
                                } else {
                                    RemoteIconSpec.Visibility
                                },
                                tint = colorScheme.onSurfaceVariant,
                                size = 22.dp
                            )
                        }
                    } else {
                        trailingIcon?.invoke()
                    }
                }
            }
        )

        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.error,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

/**
 * Campo de búsqueda con diseño específico
 */
@Composable
fun EduCoreSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    onSearch: () -> Unit = {}
) {
    EduCoreTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = RemoteIconSpec.Search,
        singleLine = true,
        imeAction = ImeAction.Search,
        onImeAction = onSearch
    )
}
