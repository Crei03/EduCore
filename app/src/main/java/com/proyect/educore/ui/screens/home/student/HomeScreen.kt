package com.proyect.educore.ui.screens.home.student

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.BluePrimary
import com.proyect.educore.ui.theme.EduCoreTheme
import com.proyect.educore.ui.theme.NeutralSurfaceVariantLight
import com.proyect.educore.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    estudianteId: Long,
    onLogout: () -> Unit,
    onNavigateToSolicitarTurno: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val turnoRepository = remember { TurnoRepository() }
    val scope = rememberCoroutineScope()

    var turnoActual by remember { mutableStateOf<Turno?>(null) }
    var posicion by remember { mutableStateOf<Int?>(null) }
    var duracionEstimada by remember { mutableStateOf<Int?>(null) }
    var estimadoInicial by remember { mutableStateOf<Int?>(null) }
    var restante by remember { mutableStateOf<Int?>(null) }
    var isLoadingTurno by remember { mutableStateOf(true) }

    fun calcularEstimado() {
        scope.launch {
            val turno = turnoActual ?: return@launch
            val pos = turnoRepository.getPosicionEnFila(turno.id)
            posicion = pos
            val duracion = turnoRepository.getTiempoEstimado(turno.tipoTramiteId)
            duracionEstimada = duracion
            val estimado = (pos.takeIf { it > 0 } ?: 1) * duracion
            if (estimadoInicial == null || estimado > (estimadoInicial ?: 0)) {
                estimadoInicial = estimado
            }
            restante = estimado
        }
    }

    LaunchedEffect(estudianteId) {
        scope.launch {
            turnoActual = turnoRepository.getTurnoActual(estudianteId)
            isLoadingTurno = false
            calcularEstimado()
            // Simulación de WebSocket: refresco periódico como si llegaran notificaciones.
            while (true) {
                kotlinx.coroutines.delay(15000)
                calcularEstimado()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Trámites escolares") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Solicitar turno") },
                icon = {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.ArrowForward,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                },
                onClick = onNavigateToSolicitarTurno
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoadingTurno) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text("Calculando tu tiempo de espera...")
                    }
                }
            } else if (turnoActual != null) {
                EsperaTurnoCard(
                    turno = turnoActual!!,
                    posicion = posicion,
                    duracionEstimada = duracionEstimada,
                    estimadoInicial = estimadoInicial,
                    restante = restante
                )
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Home,
                        size = 36.dp,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bienvenido a EduCore",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Consulta el estado de tus solicitudes o inicia un nuevo trámite cuando lo necesites.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mis turnos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Consulta el estado de tus solicitudes de turno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = onNavigateToHistorial,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Ver historial")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    Toast
                        .makeText(context, "Sesión finalizada", Toast.LENGTH_SHORT)
                        .show()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                RemoteIcon(
                    iconSpec = RemoteIconSpec.Logout,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar sesión",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentHomeScreenPreview() {
    EduCoreTheme {
        StudentHomeScreen(
            estudianteId = 1L,
            onLogout = {}
        )
    }
}

@Composable
private fun EsperaTurnoCard(
    turno: Turno,
    posicion: Int?,
    duracionEstimada: Int?,
    estimadoInicial: Int?,
    restante: Int?
) {
    val total = estimadoInicial?.takeIf { it > 0 } ?: restante ?: 0
    val restanteSeguro = restante ?: total
    val progreso = if (total > 0) 1f - (restanteSeguro.toFloat() / total.toFloat()) else 0f
    val mensajeAlerta = progreso >= 0.8f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tu turno en curso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Código: ${turno.codigoTurno} • Estado: ${turno.estado.replace('_', ' ')}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = progreso.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                trackColor = NeutralSurfaceVariantLight,
                color = BluePrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Posición: ${posicion ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val minutosRestantes = restanteSeguro
                val esperaTexto = if (minutosRestantes > 0) "${minutosRestantes} min" else "Pronto"
                Text(
                    text = "Espera aprox.: $esperaTexto",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = if (mensajeAlerta) {
                    "Tu turno se acerca, mantente atenta/o."
                } else {
                    "Actualizamos este tiempo en tiempo real conforme avanza la cola."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (mensajeAlerta) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (mensajeAlerta) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
