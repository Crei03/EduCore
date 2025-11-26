package com.proyect.educore.ui.screens.home.secretary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.proyect.educore.model.TipoTramite
import com.proyect.educore.model.Usuario
import com.proyect.educore.model.repository.EstadoOperacionResult
import com.proyect.educore.model.repository.TipoTramiteListResult
import com.proyect.educore.model.repository.TipoTramiteRepository
import com.proyect.educore.model.repository.TipoTramiteResult
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.EduCoreTheme
import kotlinx.coroutines.launch

private enum class TramiteFilter { Todos, Activos, Suspendidos }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretaryProceduresScreen(
    usuario: Usuario,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tramites = remember { mutableStateListOf<TipoTramite>() }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var statusChangeId by remember { mutableStateOf<Int?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var duracion by rememberSaveable { mutableStateOf("10") }
    var tramitePendingDelete by remember { mutableStateOf<TipoTramite?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(TramiteFilter.Todos) }
    var initialLoadDone by rememberSaveable { mutableStateOf(false) }

    fun launchMessage(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun openCreateDialog() {
        editingId = null
        nombre = ""
        descripcion = ""
        duracion = "10"
        showEditor = true
    }

    fun openEditDialog(tramite: TipoTramite) {
        editingId = tramite.id
        nombre = tramite.nombre
        descripcion = tramite.descripcion
        duracion = tramite.duracionEstimadaMin.toString()
        showEditor = true
    }

    fun upsertLocalTramite(tramite: TipoTramite) {
        val index = tramites.indexOfFirst { it.id == tramite.id }
        if (index >= 0) {
            tramites[index] = tramite
        } else {
            tramites.add(0, tramite)
        }
    }

    fun loadTramites() {
        if (isLoading) return
        isLoading = true
        coroutineScope.launch {
            when (val result = TipoTramiteRepository.obtenerTipos()) {
                is TipoTramiteListResult.Success -> {
                    tramites.clear()
                    tramites.addAll(result.tramites)
                }
                is TipoTramiteListResult.Error -> launchMessage(result.message)
            }
            isLoading = false
            initialLoadDone = true
        }
    }

    fun saveTramite() {
        val trimmedName = nombre.trim()
        val durationValue = duracion.toIntOrNull()
        if (trimmedName.length < 3) {
            launchMessage("Ingresa un nombre válido.")
            return
        }
        if (durationValue == null || durationValue <= 0) {
            launchMessage("La duración debe ser mayor a 0 minutos.")
            return
        }
        if (isSaving) return
        isSaving = true
        coroutineScope.launch {
            val result = if (editingId == null) {
                TipoTramiteRepository.crearTipo(trimmedName, descripcion.trim(), durationValue)
            } else {
                TipoTramiteRepository.actualizarTipo(editingId!!, trimmedName, descripcion.trim(), durationValue)
            }
            when (result) {
                is TipoTramiteResult.Success -> {
                    upsertLocalTramite(result.tramite)
                    showEditor = false
                    launchMessage(result.message)
                }
                is TipoTramiteResult.Error -> launchMessage(result.message)
            }
            isSaving = false
        }
    }

    fun updateEstado(tramite: TipoTramite, nuevoEstado: Int) {
        if (statusChangeId != null) return
        statusChangeId = tramite.id
        coroutineScope.launch {
            when (val result = TipoTramiteRepository.cambiarEstado(tramite.id, nuevoEstado)) {
                is EstadoOperacionResult.Success -> {
                    val updated = tramite.copy(activo = result.estado)
                    upsertLocalTramite(updated)
                    launchMessage(result.message)
                }
                is EstadoOperacionResult.Error -> launchMessage(result.message)
            }
            statusChangeId = null
        }
    }

    LaunchedEffect(Unit) { loadTramites() }

    val visibleTramites = tramites.filter { it.activo != 2 }
    val filteredTramites = visibleTramites.filter { tramite ->
        (searchQuery.isBlank() ||
            tramite.nombre.contains(searchQuery, ignoreCase = true) ||
            tramite.descripcion.contains(searchQuery, ignoreCase = true)) &&
            when (filter) {
                TramiteFilter.Todos -> true
                TramiteFilter.Activos -> tramite.activo == 1
                TramiteFilter.Suspendidos -> tramite.activo == 0
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "TRÁMITES") },
                    navigationIcon = {
                        TextButton(onClick = onNavigateBack) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.ArrowBack,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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
            },
            snackbarHost = {},
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { openCreateDialog() },
                    text = { Text(text = "Nuevo trámite") },
                    icon = {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Add,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.large
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Hola, ${usuario.nombre}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gestiona los trámites activos y suspendidos desde esta sección.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Buscar trámites") },
                            leadingIcon = {
                                RemoteIcon(
                                    iconSpec = RemoteIconSpec.Search,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true
                        )
                        Button(
                            onClick = { loadTramites() },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.Search,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(text = "Buscar", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterToggleButton(
                            label = "Activos",
                            selected = filter == TramiteFilter.Activos,
                            onClick = { filter = TramiteFilter.Activos },
                            modifier = Modifier.weight(1f)
                        )
                        FilterToggleButton(
                            label = "Suspendidos",
                            selected = filter == TramiteFilter.Suspendidos,
                            onClick = { filter = TramiteFilter.Suspendidos },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { filter = TramiteFilter.Todos }) {
                            Text(text = "Todos")
                        }
                    }
                }
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Resumen",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Activos: ${tramites.count { it.activo == 1 }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Suspendidos: ${tramites.count { it.activo == 0 }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isLoading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                if (!initialLoadDone && isLoading) {
                    item {
                        Text(
                            text = "Cargando trámites...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (filteredTramites.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RemoteIcon(
                                    iconSpec = RemoteIconSpec.List,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (initialLoadDone) "No se encontraron trámites" else "Sin datos todavía",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Ajusta la búsqueda o crea un nuevo trámite.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredTramites,
                        key = { it.id }
                    ) { tramite ->
                        TramiteCard(
                            tramite = tramite,
                            onEdit = { openEditDialog(tramite) },
                            onUpdateEstado = { newState -> updateEstado(tramite, newState) },
                            onDelete = { tramitePendingDelete = tramite },
                            isUpdating = statusChangeId == tramite.id || isSaving
                        )
                    }
                }
            }
        }

        OverlaySnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditor = false },
            title = { Text(text = if (editingId == null) "Registrar trámite" else "Editar trámite") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del trámite") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = duracion,
                        onValueChange = { input -> duracion = input.filter { it.isDigit() } },
                        label = { Text("Duración estimada (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { saveTramite() }, enabled = !isSaving) {
                    Text(text = if (editingId == null) "Guardar" else "Actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isSaving) showEditor = false }, enabled = !isSaving) {
                    Text(text = "Cancelar")
                }
            }
        )
    }

    tramitePendingDelete?.let { tramite ->
        AlertDialog(
            onDismissRequest = { tramitePendingDelete = null },
            title = { Text(text = "Eliminar trámite") },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar \"${tramite.nombre}\"? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateEstado(tramite, 2)
                    tramitePendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tramitePendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TramiteCard(
    tramite: TipoTramite,
    onEdit: (TipoTramite) -> Unit,
    onUpdateEstado: (Int) -> Unit,
    onDelete: () -> Unit,
    isUpdating: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tramite.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (tramite.descripcion.isNotBlank()) {
                Text(
                    text = tramite.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RemoteIcon(
                    iconSpec = RemoteIconSpec.Schedule,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Duración estimada: ${tramite.duracionEstimadaMin} min",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                EstadoBadge(tramite = tramite)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onUpdateEstado(1) },
                    modifier = Modifier.weight(1f),
                    enabled = tramite.activo != 1 && !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Play,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(text = "Activar", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = { onUpdateEstado(0) },
                    modifier = Modifier.weight(1f),
                    enabled = tramite.activo != 0 && !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Pause,
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                    Text(text = "Suspender", modifier = Modifier.padding(start = 4.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { onEdit(tramite) },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Edit,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(text = "Editar", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Delete,
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Text(text = "Eliminar", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun EstadoBadge(tramite: TipoTramite) {
    val (label, containerColor, textColor) = when (tramite.activo) {
        1 -> Triple(
            "Activo",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        0 -> Triple(
            "Suspendido",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            "Eliminado",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }
    Surface(
        color = containerColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FilterToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label, modifier = Modifier.padding(start = 4.dp))
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun OverlaySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState) { data ->
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                excludeFromSystemGesture = false
            )
        ) {
            Snackbar(
                snackbarData = data,
                modifier = modifier.padding(horizontal = 16.dp, vertical = 32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SecretaryProceduresScreenPreview() {
    EduCoreTheme {
        SecretaryProceduresScreen(
            usuario = Usuario(
                id = 1,
                nombre = "María",
                apellido = "García",
                email = "maria@educore.edu",
                rol = "SECRETARIA"
            ),
            onNavigateBack = {},
            onLogout = {}
        )
    }
}
