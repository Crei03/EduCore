# 🏗️ Arquitectura del Módulo 3

## 📐 Diagrama de Flujo de Navegación

```
┌──────────────────┐
│  LoginScreen     │
└────────┬─────────┘
         │ onLoginSuccess
         ▼
┌──────────────────┐
│  HomeRoute       │  <- Determina rol
└────────┬─────────┘
         │
    ┌────┴─────────────────────────────┐
    │ usuario.isStudent()               │
    │                                   │
    ▼                                   ▼
┌─────────────────────┐      ┌──────────────────┐
│ StudentHomeScreen   │      │ SecretaryRoute   │
│                     │      │ (Módulo 4)       │
│ ┌─────────────────┐ │      └──────────────────┘
│ │ FAB Solicitar   │◄───────────┐
│ │ Card Historial  │ │          │
└─────────────────────┘ │          │
    │                 │          │
    │ onClick FAB     │          │
    ▼                 │          │
┌─────────────────────────────────┐
│ SolicitarTurnoScreen            │
│                                 │
│ 1. Carga tipos de trámite       │
│ 2. Selecciona uno               │
│ 3. Calcula tiempo estimado      │
│ 4. Confirma turno               │
│ 5. Genera código único          │
└────┬────────────────────────────┘
     │ onTurnoCreated(turnoId)
     ▼
┌─────────────────────────────────┐
│ DetalleTurnoScreen              │
│                                 │
│ 1. Muestra código               │
│ 2. Posición en fila             │
│ 3. Tiempo estimado              │
│ 4. Notificaciones               │
│ 5. Botón cancelar               │
└────┬────────────────────────────┘
     │                    │
     │ Back               │ onCancelClick
     │ (popBackStack)     │
     │                    ▼
     │            ┌──────────────┐
     │            │ ConfirmDialog│
     │            │ Cancelar?    │
     │            └──────┬───────┘
     │                   │ Sí
     │                   ▼
     │            Estado → CANCELADO
     │                   │
     ▼                   ▼
┌─────────────────────────────────┐
│ HomeRoute (vuelve)              │
└────┬────────────────────────────┘
     │
     │ onClick Card Historial
     ▼
┌─────────────────────────────────┐
│ HistorialTurnosScreen           │
│                                 │
│ 1. Carga turnos del estudiante  │
│ 2. Muestra lista con filtros    │
│ 3. Filtra por estado            │
│ 4. Ordena por fecha             │
└─────────────────────────────────┘
```

---

## 🔌 Arquitectura de Capas

```
┌─────────────────────────────────────┐
│     UI LAYER (Composables)          │
├─────────────────────────────────────┤
│ SolicitarTurnoScreen                │
│ DetalleTurnoScreen                  │
│ HistorialTurnosScreen               │
└──────────────┬──────────────────────┘
               │
               │ Llama métodos
               │
┌──────────────▼──────────────────────┐
│   REPOSITORY LAYER                  │
├─────────────────────────────────────┤
│ TurnoRepository                     │
│ - getTurnosEstudiante()             │
│ - getTurnoActual()                  │
│ - crearTurno()                      │
│ - cancelarTurno()                   │
│ - getTiempoEstimado()               │
│ - getPosicionEnFila()               │
└──────────────┬──────────────────────┘
               │
               │ Usa ApiService
               │
┌──────────────▼──────────────────────┐
│   API LAYER                         │
├─────────────────────────────────────┤
│ ApiService.kt                       │
│ - fetchTurnosEstudiante()           │
│ - fetchTurnoActual()                │
│ - fetchTiempoEstimado()             │
│ - fetchPosicionEnFila()             │
│ - createTurno()                     │
│ - cancelarTurno()                   │
└──────────────┬──────────────────────┘
               │
               │ HTTP Requests
               │
┌──────────────▼──────────────────────┐
│   BACKEND (PHP)                     │
├─────────────────────────────────────┤
│ Turnos.php                          │
│ - listByEstudiante                  │
│ - getCurrent                        │
│ - create                            │
│ - updateStatus                      │
│ - estimateTime                      │
│ - getPosition                       │
└──────────────┬──────────────────────┘
               │
               │ Database Queries
               │
┌──────────────▼──────────────────────┐
│   DATABASE (MySQL)                  │
├─────────────────────────────────────┤
│ turnos                              │
│ tipos_tramite                       │
│ usuarios                            │
└─────────────────────────────────────┘
```

---

## 🔄 Diagrama de Secuencia: Crear Turno

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ UI (Screen)  │       │ Repository   │       │ Backend API  │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │
       │ criarTurno()         │                      │
       ├─────────────────────>│                      │
       │                      │                      │
       │                      │ ApiService.createTurno()
       │                      ├─────────────────────>│
       │                      │                      │
       │                      │     POST /Turnos.php
       │                      │     {estudiante_id, tipo_tramite_id}
       │                      │<─────────────────────┤
       │                      │ {success, codigo_turno}
       │                      │                      │
       │ Turno object         │                      │
       │<─────────────────────┤                      │
       │                      │                      │
       │ onTurnoCreated()     │                      │
       ├──────────┐           │                      │
       │Navigate  │           │                      │
       └──────────┘           │                      │
```

---

## 📊 Diagrama de Secuencia: Obtener Posición

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ UI (Detail)  │       │ Repository   │       │ Backend API  │       │ Database     │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │                      │
       │ LaunchedEffect()     │                      │                      │
       │ (cada 5 seg)         │                      │                      │
       │                      │                      │                      │
       │ getPosicionEnFila()  │                      │                      │
       ├─────────────────────>│                      │                      │
       │                      │                      │                      │
       │                      │ ApiService.fetchPosicionEnFila()
       │                      ├─────────────────────>│                      │
       │                      │                      │                      │
       │                      │   GET ?action=getPosition&turnoId=X
       │                      │                      ├─────────────────────>│
       │                      │                      │                      │
       │                      │                      │ SELECT COUNT(*)      │
       │                      │                      │ FROM turnos          │
       │                      │                      │ WHERE tipo=X AND     │
       │                      │                      │ estado='EN_COLA'     │
       │                      │                      │ AND hora < thisHora  │
       │                      │                      │<─────────────────────┤
       │                      │                      │ count + 1 = position │
       │                      │ {position: 2}       │                      │
       │                      │<─────────────────────┤                      │
       │ posicionEnFila=2     │                      │                      │
       │<─────────────────────┤                      │                      │
       │                      │                      │                      │
       │ updateUI()           │                      │                      │
       ├──────────┐           │                      │                      │
       │ Show: #2 │           │                      │                      │
       └──────────┘           │                      │                      │
```

---

## 🗂️ Estructura de Datos

### Modelo Turno
```kotlin
data class Turno(
    val id: Long,
    val codigoTurno: String,           // T-001, T-002, etc
    val estudianteId: Long,
    val tipoTramiteId: Int,
    val estado: String,                // EN_COLA, ATENDIENDO, ATENDIDO, CANCELADO, AUSENTE
    val horaSolicitud: String,         // 2025-11-23 14:30:00
    val horaInicioAtencion: String?,   // null si no ha comenzado
    val horaFinAtencion: String?,      // null si no ha terminado
    val observaciones: String?,        // Notas adicionales
    val creadoEn: String,
    val actualizadoEn: String,
    // Campos para UI
    val tipoTramiteNombre: String?,    // Matrícula, Pago, etc
    val posicionEnFila: Int?,          // 1, 2, 3...
    val tiempoEstimadoMin: Int?        // Minutos de espera
)

enum class EstadoTurno(val valor: String) {
    EN_COLA("EN_COLA"),
    ATENDIENDO("ATENDIENDO"),
    ATENDIDO("ATENDIDO"),
    CANCELADO("CANCELADO"),
    AUSENTE("AUSENTE")
}
```

### JSON Response Típico
```json
{
  "success": true,
  "message": "Turno creado correctamente.",
  "data": {
    "id": 42,
    "codigo_turno": "T-042",
    "estudiante_id": 5,
    "tipo_tramite_id": 2,
    "estado": "EN_COLA",
    "hora_solicitud": "2025-11-23 14:30:00",
    "hora_inicio_atencion": null,
    "hora_fin_atencion": null,
    "observaciones": null,
    "creado_en": "2025-11-23 14:30:00",
    "actualizado_en": "2025-11-23 14:30:00",
    "tipo_tramite_nombre": "Matrícula",
    "duracion_estimada_min": 10
  }
}
```

---

## 🎯 Estados y Transiciones

```
                    ┌─────────────┐
                    │  CREADO     │
                    │  EN_COLA    │
                    └──────┬──────┘
                           │
               ┌───────────┼───────────┐
               │           │           │
               │ Cancelar  │ Llamar    │ Ausente
               │ por       │ siguiente │ (no se
               │ estudiante│           │  presenta)
               │           │           │
               ▼           ▼           ▼
            CANCELADO  ATENDIENDO  AUSENTE
               │          │          │
               │          │ Finalizar│
               │          │ atención │
               │          ▼          │
               │       ATENDIDO      │
               │          │          │
               └──────────┼──────────┘
                          │
                          ▼
                      [TERMINADO]

Estados Finales: ATENDIDO, CANCELADO, AUSENTE
Nota: No se puede volver de un estado final
```

---

## 🔐 Validaciones en Cada Capa

### UI Layer
- ✅ Validar estudiante ID > 0
- ✅ Validar selección de tipo de trámite
- ✅ Mostrar loading state durante operaciones

### Repository Layer
- ✅ Validar respuestas del API
- ✅ Parsear JSON correctamente
- ✅ Manejo de excepciones
- ✅ Retornar null en caso de error

### API Layer (Kotlin)
- ✅ Validar conexión HTTP
- ✅ Validar response code (200-299)
- ✅ Timeout configurado (10 segundos)
- ✅ Charset UTF-8

### Backend Layer (PHP)
- ✅ Validar parámetros de entrada
- ✅ Validar que usuario existe
- ✅ Validar que tipo de trámite existe
- ✅ Validar estado válido
- ✅ Prepared statements para seguridad

---

## 🔄 Ciclo de Vida de un Turno

```
┌────────────────────────────────────────────────────────────┐
│ CREACIÓN (HU-07)                                           │
├────────────────────────────────────────────────────────────┤
│ 1. Estudiante selecciona tipo de trámite                   │
│ 2. Se calcula tiempo estimado (cantidad × duración)        │
│ 3. Estudiante confirma                                     │
│ 4. POST /Turnos.php?action=create                          │
│ 5. Backend genera código único (T-XXX)                     │
│ 6. Estado inicial: EN_COLA                                 │
└────────────────────────────────────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────┐
│ SEGUIMIENTO (HU-08, HU-14)                                │
├────────────────────────────────────────────────────────────┤
│ 1. Estudiante ve DetalleTurnoScreen                        │
│ 2. Cada 5 seg: GET getPosicionEnFila()                    │
│ 3. Muestra posición actual en la fila                      │
│ 4. Si faltan 2 turnos: notificación (HU-15)               │
│ 5. Recalcula tiempo estimado                              │
└────────────────────────────────────────────────────────────┘
                           ▼
         ┌─────────────────┬──────────────────┐
         │ CANCELACIÓN     │ ATENCIÓN         │
         │ (HU-09)         │ (Secretaria)     │
         └────────┬────────┴────────┬─────────┘
                  │                 │
      ┌───────────▼──────┐  ┌──────▼──────────┐
      │ Estudiante toca  │  │ Secretaria llama│
      │ "Cancelar Turno" │  │ siguiente       │
      │ con confirmación │  │ Estado→ATENDIENDO
      │ Estado→CANCELADO │  │                │
      └────────┬─────────┘  └────────┬───────┘
               │                     │
               │ Notificación        │ Se registra
               │ "Cancelado"         │ hora_inicio
               │                     │
               │                     ▼
               │            Se atiende...
               │            Secretaria finaliza
               │            Estado→ATENDIDO
               │            Se registra hora_fin
               │                     │
               └─────────────┬───────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│ FINALIZACIÓN (HU-10)                                       │
├────────────────────────────────────────────────────────────┤
│ 1. Turno aparece en HistorialTurnosScreen                 │
│ 2. Filtrable por estado: ATENDIDO, CANCELADO, AUSENTE     │
│ 3. Muestra duración real de la atención                   │
│ 4. Información persistida en base de datos                │
└────────────────────────────────────────────────────────────┘
```

---

## 💾 Modelo de Base de Datos

```sql
-- Tabla TURNOS (principal del módulo)
turnos
  ├── id (PRIMARY KEY)
  ├── codigo_turno (UNIQUE) -- T-001, T-002, etc
  ├── estudiante_id (FK → usuarios)
  ├── tipo_tramite_id (FK → tipos_tramite)
  ├── estado (ENUM) -- EN_COLA, ATENDIENDO, ATENDIDO, CANCELADO, AUSENTE
  ├── hora_solicitud (DATETIME) -- Cuando se crea el turno
  ├── hora_inicio_atencion (DATETIME, nullable) -- Cuando empieza atención
  ├── hora_fin_atencion (DATETIME, nullable) -- Cuando termina
  ├── observaciones (VARCHAR, nullable) -- Notas opcionales
  ├── creado_en (TIMESTAMP)
  ├── actualizado_en (TIMESTAMP)
  └── Índices:
      ├── idx_turnos_estado
      ├── idx_turnos_tipo_estado
      ├── idx_turnos_hora_solicitud
      └── idx_turnos_estudiante_fecha
```

---

## 🧠 Lógica de Negocio

### Cálculo de Tiempo Estimado
```
tiempo_estimado = cantidad_turnos_en_cola_para_este_tipo × duracion_estimada_del_tipo
```

### Generación de Código de Turno
```
- Obtener último código (SELECT MAX)
- Extraer número
- Incrementar +1
- Formatear como T-XXX (3 dígitos con padding)
```

### Determinación de Posición en Fila
```
SELECT COUNT(*) 
FROM turnos 
WHERE tipo_tramite_id = X 
  AND estado = 'EN_COLA'
  AND hora_solicitud < (turno_actual.hora_solicitud)

posicion = count + 1
```

---

## 🚀 Performance Optimizations

1. **LazyColumn** en listas largas de historial
2. **Índices en BD** para queries frecuentes
3. **Actualización cada 5 seg** (no más frecuente)
4. **Caching** de datos en Compose state
5. **Prepared statements** en PHP (sin SQL injection)

---

**Arquitectura completada y optimizada ✅**

