package com.proyect.educore.ui.screens.home.student.turnos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.educore.model.EstadoTurno
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.theme.BluePrimary
import com.proyect.educore.ui.theme.NeutralBackgroundLight
import com.proyect.educore.ui.theme.NeutralOutlineLight
import com.proyect.educore.ui.theme.Success
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.proyect.educore.ui.theme.EduCoreTheme

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
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else if (turnosFiltrados.isEmpty()) {
                EmptyState()
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
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterChange(null) },
            label = { Text("Todos") }
        )
        FilterChip(
            selected = selectedFilter == EstadoTurno.ATENDIDO.valor,
            onClick = { onFilterChange(EstadoTurno.ATENDIDO.valor) },
            label = { Text("Completados") }
        )
        FilterChip(
            selected = selectedFilter == EstadoTurno.CANCELADO.valor,
            onClick = { onFilterChange(EstadoTurno.CANCELADO.valor) },
            label = { Text("Cancelados") }
        )
        FilterChip(
            selected = selectedFilter == EstadoTurno.AUSENTE.valor,
            onClick = { onFilterChange(EstadoTurno.AUSENTE.valor) },
            label = { Text("Ausentes") }
        )
    }
}

@Composable
private fun TurnoHistorialCard(turno: Turno) {
    val statusIcon = when (turno.estado) {
        EstadoTurno.ATENDIDO.valor -> Icons.Filled.CheckCircle
        EstadoTurno.CANCELADO.valor -> Icons.Filled.Close
        EstadoTurno.AUSENTE.valor -> Icons.Filled.Close
        else -> Icons.Filled.Info
    }

    val statusColor = when (turno.estado) {
        EstadoTurno.ATENDIDO.valor -> Success
        EstadoTurno.CANCELADO.valor -> MaterialTheme.colorScheme.error
        EstadoTurno.AUSENTE.valor -> MaterialTheme.colorScheme.error
        else -> NeutralOutlineLight
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                Text(
                    turno.codigoTurno,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        turno.estado,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            // Tipo de trámite
            Text(
                turno.tipoTramiteNombre ?: "Tipo de trámite",
                fontSize = 13.sp,
                color = NeutralOutlineLight,
                modifier = Modifier.marginBottom(8.dp)
            )

            // Información de tiempos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Solicitado",
                    value = formatearHora(turno.horaSolicitud),
                    modifier = Modifier.weight(1f)
                )
                if (turno.horaFinAtencion?.isNotEmpty() == true) {
                    InfoItem(
                        label = "Atendido",
                        value = formatearHora(turno.horaFinAtencion),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Observaciones si las hay
            if (!turno.observaciones.isNullOrEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .marginTop(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NeutralBackgroundLight
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        turno.observaciones,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 11.sp,
                        color = NeutralOutlineLight
                    )
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
            fontSize = 10.sp,
            color = NeutralOutlineLight
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = NeutralOutlineLight,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Sin historial",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.marginTop(12.dp)
            )
            Text(
                "Aún no tienes turnos registrados",
                fontSize = 13.sp,
                color = NeutralOutlineLight,
                modifier = Modifier.marginTop(4.dp)
            )
        }
    }
}

private fun formatearHora(fechaHora: String): String {
    // Extrae solo la hora de la fecha (formato: "YYYY-MM-DD HH:MM:SS")
    return try {
        val parts = fechaHora.split(" ")
        if (parts.size > 1) parts[1].substring(0, 5) else fechaHora
    } catch (_: Exception) {
        fechaHora
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


