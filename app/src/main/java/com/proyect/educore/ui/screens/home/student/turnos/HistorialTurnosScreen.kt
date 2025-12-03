package com.proyect.educore.ui.screens.home.student.turnos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.proyect.educore.model.EstadoTurno
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.EduCoreFilterChip
import com.proyect.educore.ui.components.EduCoreStatusBadge
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.StatusType
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.cards.EduCoreEmptyCard
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

/**
 * Pantalla que muestra el historial de turnos del estudiante.
 * Lista los turnos anteriores con su estado (ATENDIDO, CANCELADO, etc.).
 *
 * @param estudianteId ID del estudiante
 * @param onBackClick Callback para volver atrás
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialTurnosScreen(
    estudianteId: Long,
    onBackClick: () -> Unit
) {
    val turnoRepository = remember { TurnoRepository() }
    val scope = rememberCoroutineScope()

    var turnos by remember { mutableStateOf<List<Turno>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val loadedTurnos = turnoRepository.getTurnosEstudiante(estudianteId)
            turnos = loadedTurnos?.sortedByDescending { it.horaSolicitud } ?: emptyList()
            isLoading = false
        }
    }

    val turnosFiltrados = if (selectedFilter != null) {
        turnos.filter { it.estado == selectedFilter }
    } else {
        turnos
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Turnos") },
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
        ) {
            // Filtros
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (turnosFiltrados.isEmpty()) {
                EduCoreEmptyCard(
                    iconSpec = RemoteIconSpec.Schedule,
                    title = "Sin historial",
                    description = "Aún no tienes turnos registrados en tu historial.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(turnosFiltrados) { turno ->
                        TurnoHistorialCard(turno = turno)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EduCoreFilterChip(
            text = "Todos",
            selected = selectedFilter == null,
            onClick = { onFilterChange(null) }
        )
        EduCoreFilterChip(
            text = "En espera",
            selected = selectedFilter == EstadoTurno.EN_COLA.valor,
            onClick = { onFilterChange(EstadoTurno.EN_COLA.valor) }
        )
        EduCoreFilterChip(
            text = "Atendidos",
            selected = selectedFilter == EstadoTurno.ATENDIDO.valor,
            onClick = { onFilterChange(EstadoTurno.ATENDIDO.valor) }
        )
        EduCoreFilterChip(
            text = "Cancelados",
            selected = selectedFilter == EstadoTurno.CANCELADO.valor,
            onClick = { onFilterChange(EstadoTurno.CANCELADO.valor) }
        )
    }
}

@Composable
private fun TurnoHistorialCard(turno: Turno) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Mapeo correcto de iconos según el estado del turno
    val (statusIcon, statusType) = when (turno.estado) {
        EstadoTurno.ATENDIDO.valor -> RemoteIconSpec.Check to StatusType.SUCCESS
        EstadoTurno.ATENDIENDO.valor -> RemoteIconSpec.Play to StatusType.INFO
        EstadoTurno.CANCELADO.valor -> RemoteIconSpec.Cancel to StatusType.ERROR
        EstadoTurno.AUSENTE.valor -> RemoteIconSpec.Block to StatusType.ERROR
        EstadoTurno.EN_COLA.valor -> RemoteIconSpec.AccessTime to StatusType.WARNING
        else -> RemoteIconSpec.Schedule to StatusType.INFO
    }

    val statusColor = when (turno.estado) {
        EstadoTurno.ATENDIDO.valor -> colorScheme.secondary
        EstadoTurno.ATENDIENDO.valor -> colorScheme.primary
        EstadoTurno.CANCELADO.valor -> colorScheme.error
        EstadoTurno.AUSENTE.valor -> colorScheme.error
        EstadoTurno.EN_COLA.valor -> colorScheme.tertiary
        else -> colorScheme.onSurfaceVariant
    }

    EduCoreCard(
        variant = CardVariant.ELEVATED,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .marginBottom(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        RemoteIcon(
                            iconSpec = statusIcon,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        turno.codigoTurno,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                EduCoreStatusBadge(
                    status = statusType,
                    text = turno.estado
                )
            }

            // Tipo de trámite
            Text(
                turno.tipoTramiteNombre ?: "Tipo de trámite",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.marginBottom(12.dp)
            )

            // Información de tiempos
            EduCoreCard(
                variant = CardVariant.FILLED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        label = "Solicitado",
                        value = formatearHora(turno.horaSolicitud),
                        modifier = Modifier.weight(1f)
                    )
                    InfoItem(
                        label = "Atendido",
                        value = formatearHora(turno.horaFinAtencion, placeholder = "En proceso"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Observaciones si las hay (omitimos mensajes internos del panel)
            val observacionLimpia = turno.observaciones?.trim().orEmpty()
            val mostrarObservacion = observacionLimpia.isNotEmpty()
                    && !observacionLimpia.equals("null", ignoreCase = true)
                    && !observacionLimpia.equals("Marcado desde panel", ignoreCase = true)
            if (mostrarObservacion) {
                EduCoreCard(
                    variant = CardVariant.OUTLINED,
                    modifier = Modifier
                        .fillMaxWidth()
                        .marginTop(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Edit,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            observacionLimpia,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatearHora(fechaHora: String): String {
    return formatearHora(fechaHora, "--")
}

private fun formatearHora(fechaHora: String?, placeholder: String): String {
    if (fechaHora.isNullOrBlank() || fechaHora.equals("null", ignoreCase = true)) return placeholder
    // Extrae solo la hora de la fecha (formato: "YYYY-MM-DD HH:MM:SS")
    return try {
        val parts = fechaHora.split(" ")
        if (parts.size > 1) parts[1].substring(0, 5) else fechaHora
    } catch (_: Exception) {
        placeholder
    }
}

private fun Modifier.marginTop(value: Dp) = this.then(Modifier.padding(top = value))
private fun Modifier.marginBottom(value: Dp) = this.then(Modifier.padding(bottom = value))

@Preview(showBackground = true)
@Composable
private fun HistorialTurnosScreenPreview() {
    EduCoreTheme {
        HistorialTurnosScreen(
            estudianteId = 1L,
            onBackClick = {}
        )
    }
}


