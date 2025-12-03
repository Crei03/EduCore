package com.proyect.educore.ui.screens.home.student.turnos

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.TipoTramite
import com.proyect.educore.model.repository.TipoTramiteRepository
import com.proyect.educore.model.repository.TurnoOperacionResult
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.ButtonVariant
import com.proyect.educore.ui.components.EduCoreButton
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.dialog.DialogType
import com.proyect.educore.ui.components.dialog.EduCoreAlertDialog
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.rememberNotificationState
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

/**
 * Pantalla para solicitar un nuevo turno.
 * El estudiante selecciona el tipo de trámite y ve el tiempo estimado.
 *
 * @param estudianteId ID del estudiante autenticado
 * @param onTurnoCreated Callback cuando se crea exitosamente un turno
 * @param onBackClick Callback para volver atrás
 */
@SuppressLint("RememberReturnType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitarTurnoScreen(
    estudianteId: Long,
    onTurnoCreated: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val turnoRepository = remember { TurnoRepository() }
    val notificationState = rememberNotificationState()

    var tiposTramite by remember { mutableStateOf<List<TipoTramite>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTramite by remember { mutableStateOf<TipoTramite?>(null) }
    var tiempoEstimado by remember { mutableStateOf(0) }
    var isCreatingTurno by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastTurnoId by remember { mutableStateOf<Long?>(null) }

    // Cargar tipos de trámite
    LaunchedEffect(Unit) {
        scope.launch {
            val tramites = TipoTramiteRepository.getTiposTramite()
            tiposTramite = tramites ?: emptyList()
            isLoading = false
        }
    }

    // Calcular tiempo estimado cuando cambia la selección
    LaunchedEffect(selectedTramite) {
        if (selectedTramite != null) {
            scope.launch {
                tiempoEstimado = turnoRepository.getTiempoEstimado(selectedTramite!!.id)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Solicitar Turno") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.ArrowBack,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    // Título e instrucción
                    Text(
                        "Selecciona el tipo de trámite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Lista de tipos de trámite
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tiposTramite) { tramite ->
                            TramiteCard(
                                tramite = tramite,
                                isSelected = selectedTramite?.id == tramite.id,
                                onClick = { selectedTramite = tramite }
                            )
                        }
                    }

                    // Panel de tiempo estimado
                    AnimatedVisibility(visible = selectedTramite != null) {
                        EduCoreCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            variant = CardVariant.GRADIENT
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RemoteIcon(
                                        iconSpec = RemoteIconSpec.Schedule,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 24.dp
                                    )
                                }
                                Column {
                                    Text(
                                        "Tiempo estimado de espera",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Aprox. $tiempoEstimado minutos",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    // Botón confirmar
                    EduCoreButton(
                        text = "Confirmar Turno",
                        onClick = {
                            if (selectedTramite != null) {
                                isCreatingTurno = true
                                scope.launch {
                                    try {
                                        when (val result = turnoRepository.crearTurno(
                                            estudianteId = estudianteId,
                                            tipoTramiteId = selectedTramite!!.id
                                        )) {
                                            is TurnoOperacionResult.Success -> {
                                                lastTurnoId = result.turno.id
                                                showSuccessDialog = true
                                            }
                                            is TurnoOperacionResult.Error -> {
                                                notificationState.showError(result.message)
                                            }
                                        }
                                    } catch (e: java.net.ConnectException) {
                                        notificationState.showError("No se puede conectar al servidor.")
                                    } catch (e: java.net.SocketTimeoutException) {
                                        notificationState.showError("El servidor no responde.")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        notificationState.showError("Error inesperado: ${e.message}")
                                    } finally {
                                        isCreatingTurno = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        variant = ButtonVariant.PRIMARY,
                        isLoading = isCreatingTurno,
                        enabled = selectedTramite != null && !isCreatingTurno,
                        fullWidth = true
                    )
                }
            }
        }

        // Host de notificaciones
        EduCoreNotificationHost(
            state = notificationState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Diálogo de éxito
    EduCoreAlertDialog(
        visible = showSuccessDialog,
        onDismiss = {
            showSuccessDialog = false
            lastTurnoId?.let { onTurnoCreated(it) }
        },
        title = "Turno creado",
        message = "Tu solicitud se registró con éxito.",
        type = DialogType.SUCCESS,
        buttonText = "Ir al inicio"
    )
}

/**
 * Componente de tarjeta para seleccionar tipo de trámite.
 */
@Composable
private fun TramiteCard(
    tramite: TipoTramite,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                tramite.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (tramite.descripcion.isNotEmpty()) {
                Text(
                    tramite.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                "Duración estimada: ${tramite.duracionEstimadaMin} min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SolicitarTurnoScreenPreview() {
    EduCoreTheme {
        SolicitarTurnoScreen(
            estudianteId = 1L,
            onTurnoCreated = {},
            onBackClick = {}
        )
    }
}

