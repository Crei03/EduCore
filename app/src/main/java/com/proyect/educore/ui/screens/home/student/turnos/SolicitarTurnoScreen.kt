package com.proyect.educore.ui.screens.home.student.turnos

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.educore.model.TipoTramite
import com.proyect.educore.model.repository.TipoTramiteRepository
import com.proyect.educore.model.repository.TurnoRepository
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.BluePrimary
import com.proyect.educore.ui.theme.EduCoreTheme
import com.proyect.educore.ui.theme.NeutralBackgroundLight
import com.proyect.educore.ui.theme.NeutralOutlineLight
import com.proyect.educore.ui.theme.Warning
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

    var tiposTramite by remember { mutableStateOf<List<TipoTramite>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTramite by remember { mutableStateOf<TipoTramite?>(null) }
    var tiempoEstimado by remember { mutableStateOf(0) }
    var isCreatingTurno by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitar Turno") },
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else {
                // Título e instrucción
                Text(
                    "Selecciona el tipo de trámite",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.marginBottom(16.dp)
                )

                // Lista de tipos de trámite
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tiposTramite) { tramite ->
                        TramiteSelectionCard(
                            tramite = tramite,
                            isSelected = selectedTramite?.id == tramite.id,
                            onClick = { selectedTramite = tramite }
                        )
                    }
                }

                // Panel de tiempo estimado
                AnimatedVisibility(visible = selectedTramite != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .marginTop(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BluePrimary.copy(alpha = 0.1f)
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
                                iconSpec = RemoteIconSpec.Schedule,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Tiempo estimado de espera",
                                    fontSize = 12.sp,
                                    color = NeutralOutlineLight
                                )
                                Text(
                                    "Aprox. $tiempoEstimado minutos",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                        }
                    }
                }

                // Mensaje de error
                if (errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .marginTop(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                // Botón confirmar
                Button(
                    onClick = {
                        if (selectedTramite != null) {
                            isCreatingTurno = true
                            errorMessage = "" // Limpiar error previo
                            scope.launch {
                                try {
                                    println("🟡 [SolicitarTurno] Iniciando creación de turno")
                                    println("🟡 [SolicitarTurno] EstudianteId: $estudianteId")
                                    println("🟡 [SolicitarTurno] TipoTramiteId: ${selectedTramite!!.id}")
                                    println("🟡 [SolicitarTurno] Nombre del trámite: ${selectedTramite!!.nombre}")

                                    val turno = turnoRepository.crearTurno(
                                        estudianteId = estudianteId,
                                        tipoTramiteId = selectedTramite!!.id
                                    )

                                    println("🟡 [SolicitarTurno] Turno recibido: $turno")

                                    if (turno != null) {
                                        println("✅ [SolicitarTurno] Turno creado exitosamente!")
                                        println("✅ [SolicitarTurno] - ID: ${turno.id}")
                                        println("✅ [SolicitarTurno] - Código: ${turno.codigoTurno}")
                                        println("✅ [SolicitarTurno] - Estado: ${turno.estado}")
                                        onTurnoCreated(turno.id)
                                    } else {
                                        println("🔴 [SolicitarTurno] El servidor retornó NULL")
                                        println("🔴 [SolicitarTurno] Posibles causas:")
                                        println("   1. El servidor no está corriendo (XAMPP/Apache)")
                                        println("   2. La base de datos no está disponible")
                                        println("   3. El estudiante o tipo de trámite no existe en BD")
                                        println("   4. Error en la URL del servidor")
                                        errorMessage = "No se pudo crear el turno. Verifica:\n" +
                                                "• Que el servidor esté corriendo (XAMPP)\n" +
                                                "• Que la base de datos esté activa\n" +
                                                "• Tu conexión de red"
                                    }
                                } catch (e: java.net.ConnectException) {
                                    println("🔴 [SolicitarTurno] Error de conexión: ${e.message}")
                                    errorMessage = "No se puede conectar al servidor.\n" +
                                            "Verifica que XAMPP esté corriendo y la URL sea correcta."
                                } catch (e: java.net.SocketTimeoutException) {
                                    println("🔴 [SolicitarTurno] Timeout: ${e.message}")
                                    errorMessage = "El servidor no responde.\n" +
                                            "Revisa tu conexión y el servidor."
                                } catch (e: Exception) {
                                    println("🔴 [SolicitarTurno] Exception: ${e.javaClass.simpleName}")
                                    println("🔴 [SolicitarTurno] Mensaje: ${e.message}")
                                    e.printStackTrace()
                                    errorMessage = "Error inesperado: ${e.javaClass.simpleName}\n${e.message}"
                                } finally {
                                    isCreatingTurno = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .marginTop(16.dp),
                    enabled = selectedTramite != null && !isCreatingTurno
                ) {
                    if (isCreatingTurno) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirmar Turno")
                    }
                }
            }
        }
    }
}


/**
 * Componente de tarjeta para seleccionar tipo de trámite.
 */
@Composable
private fun TramiteSelectionCard(
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
                color = if (isSelected) BluePrimary else NeutralOutlineLight,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BluePrimary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                tramite.nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (tramite.descripcion.isNotEmpty()) {
                Text(
                    tramite.descripcion,
                    fontSize = 12.sp,
                    color = NeutralOutlineLight,
                    modifier = Modifier.marginTop(4.dp)
                )
            }
            Text(
                "Duración estimada: ${tramite.duracionEstimadaMin} min",
                fontSize = 11.sp,
                color = Warning,
                modifier = Modifier.marginTop(8.dp)
            )
        }
    }
}

private fun Modifier.marginTop(value: Dp) = this.then(Modifier.padding(top = value))
private fun Modifier.marginBottom(value: Dp) = this.then(Modifier.padding(bottom = value))

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

