package com.proyect.educore.ui.screens.home.student.turnos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.educore.model.EstadoTurno
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.BluePrimary
import com.proyect.educore.ui.theme.EduCoreTheme
import com.proyect.educore.ui.theme.NeutralBackgroundLight
import com.proyect.educore.ui.theme.NeutralOutlineLight
import com.proyect.educore.ui.theme.Success
import com.proyect.educore.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla que muestra el detalle y estado del turno actual.
 * Permite monitorear la posición en la fila y el tiempo de espera.
 *
 * @param turnoId ID del turno a mostrar
 * @param onBackClick Callback para volver atrás
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleTurnoScreen(
    turnoId: Long,
    onBackClick: () -> Unit
) {
    val turnoRepository = remember { TurnoRepository() }
    val scope = rememberCoroutineScope()

    var turno by remember { mutableStateOf<Turno?>(null) }
    var posicionEnFila by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isCanceling by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Cargar datos del turno inicialmente y actualizar periódicamente
    LaunchedEffect(turnoId) {
        scope.launch {
            while (true) {
                try {
                    // En una aplicación real, necesitaríamos un endpoint que devuelva
                    // el turno por ID. Por ahora usamos el turno actual.
                    posicionEnFila = turnoRepository.getPosicionEnFila(turnoId)
                    isLoading = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Actualizar cada 5 segundos
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isCanceling) {
                    showCancelDialog = false
                }
            },
            title = { Text("Cancelar Turno") },
            text = { Text("¿Estás seguro de que deseas cancelar tu turno?") },
            confirmButton = {
                Button(
                    onClick = {
                        isCanceling = true
                        scope.launch {
                            val success = turnoRepository.cancelarTurno(turnoId)
                            isCanceling = false
                            if (success) {
                                showCancelDialog = false
                                delay(300)
                                onBackClick()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isCanceling
                ) {
                    if (isCanceling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Cancelar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    enabled = !isCanceling
                ) {
                    Text("Mantener")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu Turno") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(NeutralBackgroundLight)
                .padding(16.dp)
        ) {
            if (isLoading || turno == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else {
                TurnoDetailContent(
                    turno = turno!!,
                    posicionEnFila = posicionEnFila,
                    onCancelClick = { showCancelDialog = true },
                    isCanceling = isCanceling
                )
            }
        }
    }
}

@Composable
private fun TurnoDetailContent(
    turno: Turno,
    posicionEnFila: Int,
    onCancelClick: () -> Unit,
    isCanceling: Boolean
) {
    val estadoColor = when (turno.estado) {
        EstadoTurno.EN_COLA.valor -> Warning
        EstadoTurno.ATENDIENDO.valor -> BluePrimary
        EstadoTurno.ATENDIDO.valor -> Success
        EstadoTurno.CANCELADO.valor -> MaterialTheme.colorScheme.error
        else -> NeutralOutlineLight
    }

    val statusBgColor = animateColorAsState(targetValue = estadoColor.copy(alpha = 0.15f))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Código de turno destacado
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(2.dp, estadoColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = statusBgColor.value
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Tu código de turno",
                    fontSize = 12.sp,
                    color = NeutralOutlineLight
                )
                Text(
                    turno.codigoTurno,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = estadoColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                AssistChip(
                    label = { Text(turno.estado) },
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = estadoColor.copy(alpha = 0.25f),
                        labelColor = estadoColor
                    )
                )
            }
        }

        // Información del trámite
        InformationCard(
            title = "Tipo de Trámite",
            value = turno.tipoTramiteNombre ?: "Cargando...",
            iconSpec = RemoteIconSpec.List
        )

        // Posición en la fila
        if (turno.estado == EstadoTurno.EN_COLA.valor) {
            InformationCard(
                title = "Posición en la fila",
                value = "#$posicionEnFila",
                iconSpec = RemoteIconSpec.Schedule,
                highlight = true
            )

            // Notificación si faltan pocos turnos
            if (posicionEnFila in 1..2) {
                NotificationBanner(
                    message = "¡Faltan $posicionEnFila turnos para que seas atendido! Acércate a la secretaría.",
                    type = NotificationType.WARNING
                )
            }
        }

        // Tiempo estimado
        if (turno.tiempoEstimadoMin != null && turno.tiempoEstimadoMin > 0) {
            InformationCard(
                title = "Tiempo estimado de espera",
                value = "${turno.tiempoEstimadoMin} minutos",
                iconSpec = RemoteIconSpec.Schedule
            )
        }

        // Información de horarios
        if (turno.horaInicioAtencion != null && turno.horaInicioAtencion.isNotEmpty()) {
            InformationCard(
                title = "Hora de inicio",
                value = turno.horaInicioAtencion,
                iconSpec = RemoteIconSpec.Schedule
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones de acción
        if (turno.estado == EstadoTurno.EN_COLA.valor) {
            Button(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                enabled = !isCanceling
            ) {
                if (isCanceling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cancelar Turno")
                }
            }
        }

        if (turno.estado == EstadoTurno.ATENDIDO.valor) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Success.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "Trámite completado",
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                        Text(
                            "Gracias por usar nuestro servicio",
                            fontSize = 12.sp,
                            color = Success.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InformationCard(
    title: String,
    value: String,
    iconSpec: RemoteIconSpec,
    highlight: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) BluePrimary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIcon(
                iconSpec = iconSpec,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    title,
                    fontSize = 12.sp,
                    color = NeutralOutlineLight
                )
                Text(
                    value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NotificationBanner(
    message: String,
    type: NotificationType
) {
    val backgroundColor = when (type) {
        NotificationType.WARNING -> Warning.copy(alpha = 0.15f)
        NotificationType.SUCCESS -> Success.copy(alpha = 0.15f)
        NotificationType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

    val textColor = when (type) {
        NotificationType.WARNING -> Warning
        NotificationType.SUCCESS -> Success
        NotificationType.ERROR -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

enum class NotificationType {
    WARNING, SUCCESS, ERROR
}

@Preview(showBackground = true)
@Composable
private fun DetalleTurnoScreenPreview() {
    EduCoreTheme {
        DetalleTurnoScreen(
            turnoId = 1L,
            onBackClick = {}
        )
    }
}