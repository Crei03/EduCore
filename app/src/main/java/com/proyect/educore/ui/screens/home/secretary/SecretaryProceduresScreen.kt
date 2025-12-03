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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.TipoTramite
import com.proyect.educore.model.Usuario
import com.proyect.educore.model.repository.EstadoOperacionResult
import com.proyect.educore.model.repository.TipoTramiteListResult
import com.proyect.educore.model.repository.TipoTramiteRepository
import com.proyect.educore.model.repository.TipoTramiteResult
import com.proyect.educore.ui.components.EduCoreSearchField
import com.proyect.educore.ui.components.EduCoreStatusBadge
import com.proyect.educore.ui.components.EduCoreTextField
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.components.StatusType
import com.proyect.educore.ui.components.cards.CardVariant
import com.proyect.educore.ui.components.cards.EduCoreCard
import com.proyect.educore.ui.components.cards.EduCoreEmptyCard
import com.proyect.educore.ui.components.cards.EduCoreStatCard
import com.proyect.educore.ui.components.dialog.DialogType
import com.proyect.educore.ui.components.dialog.EduCoreConfirmDialog
import com.proyect.educore.ui.components.dialog.EduCoreDialog
import com.proyect.educore.ui.components.notification.EduCoreNotificationHost
import com.proyect.educore.ui.components.notification.rememberNotificationState
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
    val notificationState = rememberNotificationState()
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

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
        notificationState.showInfo(message)
    }

    fun launchSuccess(message: String) {
        notificationState.showSuccess(message)
    }

    fun launchError(message: String) {
        notificationState.showError(message)
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
                is TipoTramiteListResult.Error -> launchError(result.message)
            }
            isLoading = false
            initialLoadDone = true
        }
    }

    fun saveTramite() {
        val trimmedName = nombre.trim()
        val durationValue = duracion.toIntOrNull()
        if (trimmedName.length < 3) {
            launchError("Ingresa un nombre válido.")
            return
        }
        if (durationValue == null || durationValue <= 0) {
            launchError("La duración debe ser mayor a 0 minutos.")
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
                    launchSuccess(result.message)
                }
                is TipoTramiteResult.Error -> launchError(result.message)
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
                    launchSuccess(result.message)
                }
                is EstadoOperacionResult.Error -> launchError(result.message)
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
                    title = { Text(text = "Gestión de trámites") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            RemoteIcon(
                                iconSpec = RemoteIconSpec.ArrowBack,
                                tint = colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { openCreateDialog() },
                    text = { Text(text = "Nuevo trámite", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.Add,
                            tint = colorScheme.onPrimary
                        )
                    },
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
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Cards - Resumen mejorado
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EduCoreStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Activos",
                            value = tramites.count { it.activo == 1 }.toString(),
                            iconSpec = RemoteIconSpec.Check,
                            accentColor = colorScheme.secondary
                        )
                        EduCoreStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Suspendidos",
                            value = tramites.count { it.activo == 0 }.toString(),
                            iconSpec = RemoteIconSpec.Pause,
                            accentColor = colorScheme.tertiary
                        )
                    }
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Búsqueda
                item {
                    EduCoreSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Buscar trámites...",
                        onSearch = { loadTramites() }
                    )
                }

                // Filtros
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

                if (!initialLoadDone && isLoading) {
                    item {
                        Text(
                            text = "Cargando trámites...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (filteredTramites.isEmpty()) {
                    item {
                        EduCoreEmptyCard(
                            modifier = Modifier.fillMaxWidth(),
                            iconSpec = RemoteIconSpec.List,
                            title = if (initialLoadDone) "No se encontraron trámites" else "Sin datos todavía",
                            description = "Ajusta la búsqueda o crea un nuevo trámite."
                        )
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

        // Host de notificaciones
        EduCoreNotificationHost(
            state = notificationState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Diálogo de edición/creación de trámites
    EduCoreDialog(
        visible = showEditor,
        onDismiss = { if (!isSaving) showEditor = false },
        title = if (editingId == null) "Registrar trámite" else "Editar trámite",
        message = "Completa los datos del trámite",
        type = DialogType.INFO,
        confirmText = if (editingId == null) "Guardar" else "Actualizar",
        cancelText = "Cancelar",
        onConfirm = { saveTramite() },
        onCancel = { if (!isSaving) showEditor = false },
        isLoading = isSaving
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EduCoreTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Nombre del trámite",
                modifier = Modifier.fillMaxWidth()
            )
            EduCoreTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = "Descripción",
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )
            EduCoreTextField(
                value = duracion,
                onValueChange = { input -> duracion = input.filter { it.isDigit() } },
                label = "Duración estimada (min)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Diálogo de confirmación de eliminación
    EduCoreConfirmDialog(
        visible = tramitePendingDelete != null,
        onDismiss = { tramitePendingDelete = null },
        title = "Eliminar trámite",
        message = "¿Seguro que deseas eliminar \"${tramitePendingDelete?.nombre}\"? Esta acción no se puede deshacer.",
        onConfirm = {
            tramitePendingDelete?.let { tramite ->
                updateEstado(tramite, 2)
            }
            tramitePendingDelete = null
        },
        confirmText = "Eliminar",
        cancelText = "Cancelar",
        isDestructive = true
    )
}

@Composable
private fun TramiteCard(
    tramite: TipoTramite,
    onEdit: (TipoTramite) -> Unit,
    onUpdateEstado: (Int) -> Unit,
    onDelete: () -> Unit,
    isUpdating: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isActive = tramite.activo == 1

    EduCoreCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.ELEVATED
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tramite.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (tramite.descripcion.isNotBlank()) {
                    Text(
                        text = tramite.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            EstadoBadge(tramite = tramite)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RemoteIcon(
                iconSpec = RemoteIconSpec.Schedule,
                tint = colorScheme.onSurfaceVariant,
                size = 18.dp
            )
            Text(
                text = "${tramite.duracionEstimadaMin} min estimados",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botones de acción - Diseño mejorado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón Activar/Suspender (principal)
            if (isActive) {
                OutlinedButton(
                    onClick = { onUpdateEstado(0) },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.tertiary
                    ),
                    border = BorderStroke(1.dp, colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Pause,
                        tint = colorScheme.tertiary,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Suspender")
                }
            } else {
                Button(
                    onClick = { onUpdateEstado(1) },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary,
                        contentColor = colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    RemoteIcon(
                        iconSpec = RemoteIconSpec.Play,
                        tint = colorScheme.onSecondary,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Activar")
                }
            }

            // Botón Editar
            FilledTonalButton(
                onClick = { onEdit(tramite) },
                enabled = !isUpdating,
                shape = RoundedCornerShape(12.dp)
            ) {
                RemoteIcon(
                    iconSpec = RemoteIconSpec.Edit,
                    tint = colorScheme.onSecondaryContainer,
                    size = 18.dp
                )
            }

            // Botón Eliminar
            OutlinedButton(
                onClick = onDelete,
                enabled = !isUpdating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorScheme.error
                ),
                border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                RemoteIcon(
                    iconSpec = RemoteIconSpec.Delete,
                    tint = colorScheme.error,
                    size = 18.dp
                )
            }
        }
    }
}

@Composable
private fun EstadoBadge(tramite: TipoTramite) {
    val (label, statusType) = when (tramite.activo) {
        1 -> "Activo" to StatusType.SUCCESS
        0 -> "Suspendido" to StatusType.WARNING
        else -> "Eliminado" to StatusType.ERROR
    }
    EduCoreStatusBadge(
        text = label,
        status = statusType
    )
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
