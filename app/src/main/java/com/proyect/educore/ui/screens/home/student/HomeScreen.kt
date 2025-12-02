package com.proyect.educore.ui.screens.home.student

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Turno
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.cards.EduCoreFeatureCard
import com.proyect.educore.ui.components.drawer.DrawerMenuItem
import com.proyect.educore.ui.components.drawer.EduCoreDrawer
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.rememberNotificationState
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    estudianteId: Long,
    estudianteNombre: String = "Estudiante",
    onLogout: () -> Unit,
    onNavigateToSolicitarTurno: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {}
) {
    val notificationState = rememberNotificationState()
    val turnoRepository = remember { TurnoRepository() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val colorScheme = MaterialTheme.colorScheme

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

    // Items del menú para el drawer
    val menuItems = listOf(
        DrawerMenuItem("home", "Inicio", RemoteIconSpec.Home),
        DrawerMenuItem("history", "Mis turnos", RemoteIconSpec.History)
    )

    EduCoreDrawer(
        drawerState = drawerState,
        userName = estudianteNombre,
        userRole = "Estudiante",
        menuItems = menuItems,
        selectedItemId = "home",
        onItemClick = { item ->
            scope.launch {
                drawerState.close()
                when (item.id) {
                    "history" -> onNavigateToHistorial()
                }
            }
        },
        onLogout = {
            scope.launch {
                drawerState.close()
                notificationState.showInfo("Sesión finalizada")
                onLogout()
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = "Trámites escolares") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                RemoteIcon(
                                    iconSpec = RemoteIconSpec.Menu,
                                    tint = colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { 
                            Text(
                                "Solicitar turno",
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        icon = {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.Add,
                                tint = colorScheme.onPrimary
                            )
                        },
                        onClick = onNavigateToSolicitarTurno,
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        ),
                        shape = RoundedCornerShape(16.dp)
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
                    // 1. Card de bienvenida destacada (primero)
                    EduCoreCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = CardVariant.GRADIENT
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                RemoteIcon(
                                    iconSpec = RemoteIconSpec.School,
                                    size = 28.dp,
                                    tint = colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¡Hola, $estudianteNombre!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = "Consulta tus turnos o solicita uno nuevo cuando lo necesites.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // 2. Card de turno actual (segundo)
                    if (isLoadingTurno) {
                        EduCoreCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.FILLED
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = colorScheme.primary
                                )
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

                    // 3. Card de historial (tercero)
                    EduCoreFeatureCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Mis turnos",
                        description = "Consulta el historial y estado de tus solicitudes.",
                        iconSpec = RemoteIconSpec.History,
                        onClick = onNavigateToHistorial,
                        accentColor = colorScheme.secondary
                    )
                }
            }

            // Host de notificaciones
            EduCoreNotificationHost(
                state = notificationState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentHomeScreenPreview() {
    EduCoreTheme {
        StudentHomeScreen(
            estudianteId = 1L,
            estudianteNombre = "Carlos",
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
    val enAtencion = turno.estado.equals("ATENDIENDO", ignoreCase = true)
    val progressColor = if (enAtencion) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val colorScheme = MaterialTheme.colorScheme

    EduCoreCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.ELEVATED
    ) {
        Text(
            text = "Tu turno en curso",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Código: ${turno.codigoTurno} • Estado: ${turno.estado.replace('_', ' ')}",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progreso.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp)),
            trackColor = colorScheme.surfaceVariant,
            color = progressColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Posición: ${posicion ?: "--"}",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
            val minutosRestantes = restanteSeguro
            val esperaTexto = if (minutosRestantes > 0) "${minutosRestantes} min" else "Pronto"
            Text(
                text = "Espera aprox.: $esperaTexto",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (mensajeAlerta) {
                "Tu turno se acerca, mantente atenta/o."
            } else {
                "Actualizamos este tiempo en tiempo real conforme avanza la cola."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (mensajeAlerta) colorScheme.tertiary else colorScheme.onSurfaceVariant,
            fontWeight = if (mensajeAlerta) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
