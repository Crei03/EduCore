package com.proyect.educore.ui.screens.home.student.turnos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.educore.model.EstadoTurno
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.ButtonVariant
import com.proyect.educore.ui.components.EduCoreButton
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.dialog.DialogType
import com.proyect.educore.ui.components.dialog.EduCoreConfirmDialog
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.rememberNotificationState
import com.proyect.educore.ui.theme.EduCoreTheme
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
    val notificationState = rememberNotificationState()

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

    // Diálogo de confirmación para cancelar
    if (showCancelDialog) {
        EduCoreConfirmDialog(
            visible = true,
            title = "Cancelar Turno",
            message = "¿Estás seguro de que deseas cancelar tu turno? Esta acción no se puede deshacer.",
            confirmText = "Cancelar turno",
            cancelText = "Mantener",
            isDestructive = true,
            isLoading = isCanceling,
            onConfirm = {
                isCanceling = true
                scope.launch {
                    val success = turnoRepository.cancelarTurno(turnoId)
                    isCanceling = false
                    if (success) {
                        showCancelDialog = false
                        notificationState.showSuccess("Turno cancelado correctamente")
                        delay(800)
                        onBackClick()
                    } else {
                        notificationState.showError("No se pudo cancelar el turno")
                    }
                }
            },
            onDismiss = {
                if (!isCanceling) {
                    showCancelDialog = false
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tu Turno") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.ArrowBack,
                                contentDescription = "Volver"
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
                if (isLoading || turno == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
        
        EduCoreNotificationHost(
            state = notificationState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun TurnoDetailContent(
    turno: Turno,
    posicionEnFila: Int,
    onCancelClick: () -> Unit,
    isCanceling: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val estadoColor = when (turno.estado) {
        EstadoTurno.EN_COLA.valor -> colorScheme.tertiary
        EstadoTurno.ATENDIENDO.valor -> colorScheme.primary
        EstadoTurno.ATENDIDO.valor -> colorScheme.secondary
        EstadoTurno.CANCELADO.valor -> colorScheme.error
        else -> colorScheme.onSurfaceVariant
    }

    val statusBgColor = animateColorAsState(targetValue = estadoColor.copy(alpha = 0.1f), label = "statusBg")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Código de turno destacado - versión moderna
        EduCoreCard(
            variant = CardVariant.GLASS,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, estadoColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .background(statusBgColor.value, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tu código de turno",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        turno.codigoTurno,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = estadoColor,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = estadoColor.copy(alpha = 0.2f),
                        contentColor = estadoColor,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = turno.estado,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Información del trámite
        InformationCard(
            title = "Tipo de Trámite",
            value = turno.tipoTramiteNombre ?: "Cargando...",
            iconSpec = RemoteIconSpec.List
        )

        if (turno.estado == EstadoTurno.ATENDIENDO.valor) {
            EduCoreCard(
                variant = CardVariant.GRADIENT,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Play,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "En proceso",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Tu trámite está siendo atendido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Posición en la fila
        if (turno.estado == EstadoTurno.EN_COLA.valor) {
            InformationCard(
                title = "Posición en la fila",
                value = "#$posicionEnFila",
                iconSpec = RemoteIconSpec.List,
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
            EduCoreButton(
                text = if (isCanceling) "Cancelando..." else "Cancelar Turno",
                onClick = onCancelClick,
                variant = ButtonVariant.DESTRUCTIVE,
                enabled = !isCanceling,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (turno.estado == EstadoTurno.ATENDIDO.valor) {
            EduCoreCard(
                variant = CardVariant.FILLED,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Check,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            "Trámite completado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Gracias por usar nuestro servicio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
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
    EduCoreCard(
        variant = if (highlight) CardVariant.FILLED else CardVariant.OUTLINED,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (highlight) Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) else Modifier
                )
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                RemoteIcon(
                    iconSpec = iconSpec,
                    modifier = Modifier.size(24.dp),
                    tint = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface
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
    val colorScheme = MaterialTheme.colorScheme
    val (iconSpec, accentColor) = when (type) {
        NotificationType.WARNING -> RemoteIconSpec.Schedule to colorScheme.tertiary
        NotificationType.SUCCESS -> RemoteIconSpec.Play to colorScheme.secondary
        NotificationType.ERROR -> RemoteIconSpec.Delete to colorScheme.error
    }

    EduCoreCard(
        variant = CardVariant.FILLED,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.1f))
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                RemoteIcon(
                    iconSpec = iconSpec,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                message,
                color = accentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
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
