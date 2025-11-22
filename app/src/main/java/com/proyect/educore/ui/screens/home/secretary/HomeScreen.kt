package com.proyect.educore.ui.screens.home.secretary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proyect.educore.model.Usuario
import com.proyect.educore.ui.components.RemoteIcon
import com.proyect.educore.ui.components.RemoteIconSpec
import com.proyect.educore.ui.theme.EduCoreTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretaryHomeScreen(
    usuario: Usuario,
    onNavigateToProcedures: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Panel de secretaría") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Hola, ${usuario.nombre}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Administra y crea nuevos trámites para los estudiantes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Trámites",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Consulta, filtra y gestiona los trámites activos y suspendidos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = onNavigateToProcedures,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                    ) {
                        RemoteIcon(
                            iconSpec = RemoteIconSpec.List,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trámites",
                            modifier = Modifier.padding(start = 0.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Acciones rápidas",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Mantén la información de los estudiantes organizada desde aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                RemoteIcon(
                    iconSpec = RemoteIconSpec.Logout,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cerrar sesión",
                    modifier = Modifier.padding(start = 8.dp),
                    textAlign = TextAlign.Center
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
            onLogout = {}
        )
    }
}
