package com.proyect.educore.ui.screens.home.secretary

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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Usuario
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.cards.EduCoreFeatureCard
import com.proyect.educore.ui.components.cards.EduCoreStatCard
import com.proyect.educore.ui.components.drawer.DrawerMenuItem
import com.proyect.educore.ui.components.drawer.EduCoreDrawer
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretaryHomeScreen(
    usuario: Usuario,
    onNavigateToProcedures: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    // Items del menú para el drawer
    val menuItems = listOf(
        DrawerMenuItem("home", "Panel principal", RemoteIconSpec.Dashboard),
        DrawerMenuItem("queue", "Panel de atención", RemoteIconSpec.Queue),
        DrawerMenuItem("procedures", "Trámites", RemoteIconSpec.Assignment)
    )

    EduCoreDrawer(
        drawerState = drawerState,
        userName = "${usuario.nombre} ${usuario.apellido}",
        userRole = "Secretaría",
        menuItems = menuItems,
        selectedItemId = "home",
        onItemClick = { item ->
            scope.launch {
                drawerState.close()
                when (item.id) {
                    "queue" -> onNavigateToQueue()
                    "procedures" -> onNavigateToProcedures()
                }
            }
        },
        onLogout = {
            scope.launch {
                drawerState.close()
                onLogout()
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Panel de secretaría") },
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
                // Header de bienvenida destacado
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
                                iconSpec = RemoteIconSpec.PersonOutline,
                                size = 28.dp,
                                tint = colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Hola, ${usuario.nombre}!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                            Text(
                                text = "Gestiona los turnos y trámites de estudiantes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EduCoreStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Pendientes",
                        value = "--",
                        iconSpec = RemoteIconSpec.HourglassEmpty,
                        accentColor = colorScheme.tertiary
                    )
                    EduCoreStatCard(
                        modifier = Modifier.weight(1f),
                        title = "Hoy",
                        value = "--",
                        iconSpec = RemoteIconSpec.CheckOutlined,
                        accentColor = colorScheme.secondary
                    )
                }

                // Card de turno actual
                EduCoreCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.OUTLINED
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                RemoteIcon(
                                    iconSpec = RemoteIconSpec.AccessTime,
                                    size = 20.dp,
                                    tint = colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = "Turno en atención",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Sin turno activo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card principal: Panel de atención
                EduCoreFeatureCard(
                    iconSpec = RemoteIconSpec.Play,
                    title = "Panel de atención",
                    description = "Revisa la cola del día, llama al siguiente estudiante y registra el tiempo de atención.",
                    onClick = onNavigateToQueue,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = colorScheme.primary
                )

                // Card secundaria: Trámites
                EduCoreFeatureCard(
                    iconSpec = RemoteIconSpec.Assignment,
                    title = "Gestión de trámites",
                    description = "Consulta, filtra y gestiona los trámites activos y suspendidos.",
                    onClick = onNavigateToProcedures,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SecretaryHomeScreenPreview() {
    EduCoreTheme {
        SecretaryHomeScreen(
            usuario = Usuario(
                id = 1,
                nombre = "María",
                apellido = "García",
                email = "maria@educore.edu",
                rol = "SECRETARIA"
            ),
            onNavigateToProcedures = {},
            onNavigateToQueue = {},
            onLogout = {}
        )
    }
}
