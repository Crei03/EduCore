package com.proyect.educore.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

enum class RemoteIconSpec(
    internal val path: String,
    val defaultContentDescription: String
) {
    Home("home", "Icono inicio"),
    Person("person", "Icono usuario"),
    PersonAdd("person_add", "Icono crear usuario"),
    Login("login", "Icono iniciar sesión"),
    Logout("logout", "Icono cerrar sesión"),
    ExitToApp("meeting_room", "Icono salir"),
    Menu("menu", "Icono menú"),
    Email("mail", "Icono correo"),
    Lock("lock", "Icono candado"),
    Visibility("visibility", "Icono mostrar"),
    VisibilityOff("visibility_off", "Icono ocultar"),
    ArrowForward("arrow_forward", "Icono siguiente"),
    School("school", "Icono escuela"),
    ArrowBack("arrow_back", "Icono regresar"),
    Add("add", "Icono agregar"),
    List("list_alt", "Icono lista"),
    Edit("edit", "Icono editar"),
    Delete("delete", "Icono eliminar"),
    Play("play_arrow", "Icono activar"),
    Pause("pause_circle", "Icono suspender"),
    Schedule("schedule", "Icono tiempo"),
    Search("search", "Icono buscar"),
    Filter("filter_list", "Icono filtrar"),
    Check("check_circle", "Icono completado"),
    CheckOutlined("done", "Icono verificado"),
    Close("close", "Icono cerrar"),
    Cancel("cancel", "Icono cancelar"),
    Block("block", "Icono bloquear"),
    Info("info", "Icono información"),
    Warning("warning", "Icono advertencia"),
    Error("error", "Icono error"),
    Queue("format_list_numbered", "Icono cola"),
    History("history", "Icono historial"),
    Description("description", "Icono documento"),
    Notifications("notifications", "Icono notificaciones"),
    Settings("settings", "Icono configuración"),
    Refresh("refresh", "Icono actualizar"),
    MoreVert("more_vert", "Icono más opciones"),
    Star("star", "Icono estrella"),
    Help("help", "Icono ayuda"),
    PersonOutline("person_outline", "Icono persona"),
    Assignment("assignment", "Icono asignación"),
    Dashboard("dashboard", "Icono panel"),
    AccessTime("access_time", "Icono reloj"),
    HourglassEmpty("hourglass_empty", "Icono espera"),
    Groups("groups", "Icono grupos");

    val url: String = "https://fonts.gstatic.com/s/i/materialiconsoutlined/$path/v1/24px.svg"
}

@Composable
fun RemoteIcon(
    iconSpec: RemoteIconSpec,
    modifier: Modifier = Modifier,
    tint: Color? = MaterialTheme.colorScheme.onSurface,
    size: Dp = 24.dp,
    contentDescription: String? = iconSpec.defaultContentDescription
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(iconSpec.url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.extraSmall),
        imageLoader = imageLoader,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.Fit
    )
}
