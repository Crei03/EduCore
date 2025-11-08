# Instrucciones para el proyecto "Trámites Escolares"
Crea un proyecto Android en Kotlin con Jetpack Compose y Material 3 para “Trámites Escolares”, con arquitectura simplificada (UI Layer y Data Layer), navegación por pantallas, consumo de un backend PHP ubicado en /backend (mismo repo). Usa Google Fonts para la tipografía e iconos Material Symbols como fuente. Define la paleta de colores como variables reutilizables (incluye blanco y negro).

## Estructura de carpetas (simple para semestral)

```
/backend

app/src/main/java/com/proyect/educore
    ├── data
    │   ├── api
    │   │   └── ApiService.kt  
    ├── model
    │   ├── Estudiante.kt
    │   ├── repository
    │   │   └── EstudianteRepository.kt
    ├── ui
    │   ├── theme
    │   │   ├── Color.kt
    │   │   ├── Typography.kt
    │   │   └── Theme.kt
    │   ├── components
    │   ├── navigation
    │   │   ├── NavGraph.kt
    │   ├── screens

```
## Dependencias
- Compose Material 3
- Activity Compose
- Navigation Compose
- Icons (Google Fonts): ui-text-google-fonts

## Paleta de colores
### Colores base:
```
BluePrimary = #072BF2
BlueLight1 = #B3BDF2
BlueLight2 = #4B75F2
BlueLight3 = #578BF2
GrayNeutral = #C5D0D9
White = #FFFFFF
Black = #000000
```
### Asignación de colores para layout:
```
primary: BluePrimary
onPrimary: White
secondary: BlueLight2
tertiary: BlueLight3
background: White (claro) / Black (oscuro)
surface: White (claro) / #121212 (oscuro)
outline/outlineVariant: GrayNeutral
onBackground / onSurface: Black (claro) / White (oscuro)
```
### Tipografía
Usa Google Fonts con GoogleFont.Provider (Roboto) en Typography.kt
Para iconos:
- Habilita material-icons-extended (vectoriales).