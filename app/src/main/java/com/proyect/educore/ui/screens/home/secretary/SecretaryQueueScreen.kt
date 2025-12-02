package com.proyect.educore.ui.screens.home.secretary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoOperacionResult
import com.proyect.educore.model.repository.TurnoPanelResult
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.ButtonVariant
import com.proyect.educore.ui.components.EduCoreButton
import com.proyect.educore.ui.components.EduCoreStatusBadge
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.StatusType
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.cards.EduCoreEmptyCard
import com.proyect.educore.ui.components.dialog.EduCoreBottomSheet
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.rememberNotificationState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretaryQueueScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationState = rememberNotificationState()
    val scope = rememberCoroutineScope()
    val repository = remember { TurnoRepository() }

    val turnos = remember { mutableStateListOf<Turno>() }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isCallingNext by rememberSaveable { mutableStateOf(false) }
    var isOperating by rememberSaveable { mutableStateOf(false) }
    var selectedTurno by remember { mutableStateOf<Turno?>(null) }
    var showDetail by remember { mutableStateOf(false) }

    fun updateTurnoLocal(turno: Turno) {
        val index = turnos.indexOfFirst { it.id == turno.id }
        if (index >= 0) {
            turnos[index] = turno
        } else {
            turnos.add(turno)
        }
        turnos.sortBy { it.horaSolicitud }
    }

    fun loadQueue() {
        if (isLoading) return
        isLoading = true
        scope.launch {
            when (val result = repository.obtenerTurnosPanel()) {
                is TurnoPanelResult.Success -> {
                    turnos.clear()
                    turnos.addAll(result.turnos)
                }
                is TurnoPanelResult.Error -> notificationState.showError(result.message)
            }
            isLoading = false
        }
    }

    fun callNext() {
        if (isCallingNext) return
        isCallingNext = true
        scope.launch {
            when (val result = repository.llamarSiguienteTurno()) {
                is TurnoOperacionResult.Success -> {
                    updateTurnoLocal(result.turno)
                    notificationState.showSuccess(result.message + " Notificación enviada al estudiante.")
                    selectedTurno = result.turno
                    showDetail = true
                    loadQueue()
                }
                is TurnoOperacionResult.Error -> notificationState.showError(result.message)
            }
            isCallingNext = false
        }
    }

    fun finalizar(turno: Turno) {
        if (isOperating) return
        isOperating = true
        scope.launch {
            when (val result = repository.finalizarAtencion(turno.id)) {
                is TurnoOperacionResult.Success -> {
                    notificationState.showSuccess(result.message)
                    loadQueue()
                    showDetail = false
                }
                is TurnoOperacionResult.Error -> notificationState.showError(result.message)
            }
            isOperating = false
        }
    }

    fun cancelar(turno: Turno) {
        if (isOperating) return
        isOperating = true
        scope.launch {
            when (val result = repository.marcarAusente(turno.id, "Marcado desde panel")) {
                is TurnoOperacionResult.Success -> {
                    notificationState.showWarning(result.message)
                    loadQueue()
                    showDetail = false
                }
                is TurnoOperacionResult.Error -> notificationState.showError(result.message)
            }
            isOperating = false
        }
    }

    LaunchedEffect(Unit) { loadQueue() }

    // Bottom sheet para detalle del turno
    if (showDetail && selectedTurno != null) {
        val turno = selectedTurno!!
        EduCoreBottomSheet(
            visible = true,
            onDismiss = { showDetail = false },
            title = "Detalle del turno",
            showCloseButton = true
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información del estudiante
                EduCoreCard(
                    variant = CardVariant.FILLED,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.Person,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Estudiante",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = turno.nombreCompleto(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.List,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "Trámite",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = turno.tipoTramiteNombre ?: "Sin nombre",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Estado y tiempos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EduCoreCard(
                        variant = CardVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Estado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            EduCoreStatusBadge(
                                status = mapEstadoToStatusType(turno.estado),
                                text = estadoLegible(turno.estado)
                            )
                        }
                    }
                    
                    EduCoreCard(
                        variant = CardVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Solicitado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = formatFechaHora(turno.horaSolicitud),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Tiempos de atención
                EduCoreCard(
                    variant = CardVariant.OUTLINED,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow(
                            "Inicio de atención",
                            formatFechaHora(turno.horaInicioAtencion, placeholder = "Aún no iniciada")
                        )
                        DetailRow(
                            "Fin de atención",
                            formatFechaHora(turno.horaFinAtencion, placeholder = "Aún no finalizada")
                        )
                        turno.observaciones?.takeIf { it.isNotBlank() }?.let {
                            DetailRow("Notas", it)
                        }
                    }
                }
                
                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EduCoreButton(
                        text = "Marcar cancelado",
                        onClick = { cancelar(turno) },
                        variant = ButtonVariant.OUTLINE,
                        enabled = !isOperating && !turno.estado.equals("ATENDIDO", ignoreCase = true),
                        modifier = Modifier.weight(1f)
                    )
                    
                    val puedeFinalizar = turno.estado.equals("ATENDIENDO", ignoreCase = true)
                    EduCoreButton(
                        text = "Finalizar atención",
                        onClick = { finalizar(turno) },
                        variant = ButtonVariant.PRIMARY,
                        enabled = puedeFinalizar && !isOperating,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Panel de atención") },
                    navigationIcon = {
                        TextButton(onClick = onNavigateBack) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.ArrowBack,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Volver", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        TextButton(onClick = onLogout) {
                            Text("Salir", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Turnos del día",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Visualiza los turnos EN COLA y atiende en orden de solicitud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EduCoreButton(
                    text = if (isCallingNext) "Llamando..." else "Llamar siguiente",
                    onClick = { callNext() },
                    variant = ButtonVariant.PRIMARY,
                    enabled = !isCallingNext && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (turnos.isEmpty()) {
                    EduCoreEmptyCard(
                        iconSpec = RemoteIconSpec.List,
                        title = "No hay turnos en cola",
                        description = "Cuando un estudiante solicite un turno aparecerá aquí para que puedas llamarlo.",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(turnos, key = { it.id }) { turno ->
                            TurnoCard(
                                turno = turno,
                                onClick = {
                                    selectedTurno = turno
                                    showDetail = true
                                }
                            )
                        }
                    }
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
private fun TurnoCard(
    turno: Turno,
    onClick: () -> Unit
) {
    EduCoreCard(
        variant = CardVariant.ELEVATED,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turno.nombreCompleto(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Trámite: ${turno.tipoTramiteNombre ?: turno.tipoTramiteId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EduCoreStatusBadge(
                    status = mapEstadoToStatusType(turno.estado),
                    text = estadoLegible(turno.estado)
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Solicitado: ${formatFechaHora(turno.horaSolicitud)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (turno.estado.equals("ATENDIENDO", ignoreCase = true) && turno.horaInicioAtencion != null) {
                Text(
                    text = "Atendiendo desde: ${formatFechaHora(turno.horaInicioAtencion)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun mapEstadoToStatusType(estado: String): StatusType {
    return when (estado.uppercase(Locale.getDefault())) {
        "ATENDIENDO" -> StatusType.INFO
        "ATENDIDO" -> StatusType.SUCCESS
        "CANCELADO", "AUSENTE" -> StatusType.ERROR
        else -> StatusType.WARNING
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun Turno.nombreCompleto(): String {
    val nombre = estudianteNombre.orEmpty().trim()
    val apellido = estudianteApellido.orEmpty().trim()
    return when {
        nombre.isNotEmpty() && apellido.isNotEmpty() -> "$nombre $apellido"
        nombre.isNotEmpty() -> nombre
        else -> "Estudiante #$estudianteId"
    }
}

private fun formatFechaHora(raw: String?, placeholder: String = "--"): String {
    if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) return placeholder
    val limpio = raw.replace('T', ' ')
    val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val output = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return runCatching {
        output.format(input.parse(limpio) ?: return@runCatching limpio)
    }.getOrElse { limpio }
}

private fun estadoLegible(estado: String): String {
    return when (estado.uppercase(Locale.getDefault())) {
        "ATENDIENDO" -> "Atendiendo"
        "ATENDIDO" -> "Atendido"
        "CANCELADO" -> "Cancelado"
        "AUSENTE" -> "Ausente"
        else -> "En cola"
    }
}
