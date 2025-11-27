# 🧪 Testing del Módulo 3 - Guía Rápida

## 📋 Requisitos Previos

1. Backend PHP corriendo en `localhost` o servidor configurado
2. Base de datos `turnos_academia` con tablas creadas
3. Usuario de prueba registrado como Estudiante
4. Al menos 2-3 Tipos de Trámite creados

---

## 🔧 Pasos para Probar

### 1. **Iniciar Sesión como Estudiante**
```
Email: estudiante@test.com
Contraseña: test123
```
→ Deberías ver HomeScreen con FAB "Solicitar turno"

---

### 2. **Probar: Solicitar Turno (HU-07 & HU-08)**

**Acciones:**
1. Toca FAB "Solicitar turno"
2. Se carga lista de tipos de trámite
3. Selecciona uno (se destaca con borde azul)
4. Observa: Tiempo estimado se calcula automáticamente
   ```
   Fórmula: cantidad_turnos_en_cola × duracion_min
   Ej: 3 turnos × 15 min = 45 minutos
   ```
5. Toca "Confirmar Turno"
6. Se genera código único (T-001, T-002, etc.)
7. Automáticamente vas a **DetalleTurnoScreen**

**Qué validar:**
- ✅ Lista no está vacía
- ✅ Tiempo estimado es > 0
- ✅ Código de turno se genera
- ✅ Estado inicial es "EN_COLA"

---

### 3. **Probar: Detalle de Turno (HU-14)**

**En la pantalla de detalle deberías ver:**

```
┌─────────────────────────────┐
│ Tu código: T-023            │
│ [EN_COLA]                   │
│                             │
│ 📋 Tipo de Trámite: Matrícula│
│ #2 Posición en la fila      │
│ ⏱️  Tiempo: 30 minutos       │
│                             │
│ ⚠️ Notificación:             │
│ "¡Faltan 2 turnos! Acércate"│
│                             │
│ [Cancelar Turno]            │
└─────────────────────────────┘
```

**Qué validar:**
- ✅ Código de turno visible y destacado
- ✅ Estado mostrado con color correcto
- ✅ Posición en fila es correcta (número ascendente)
- ✅ Tiempo estimado > 0
- ✅ Si quedan 1-2 turnos, aparece notificación
- ✅ La pantalla se actualiza cada 5 segundos

---

### 4. **Probar: Cancelar Turno (HU-09)**

**Acciones:**
1. En DetalleTurnoScreen, toca "Cancelar Turno"
2. Aparece diálogo confirmando la acción
3. Toca "Cancelar" en el diálogo
4. Estado cambia a "CANCELADO"
5. Se muestra mensaje de éxito
6. Vuelves automáticamente a Home

**Qué validar:**
- ✅ Diálogo de confirmación aparece
- ✅ Si cancelas, estado cambia a CANCELADO
- ✅ Feedback visual del cambio
- ✅ Vuelves a la pantalla anterior

---

### 5. **Probar: Ver Historial (HU-10)**

**Acciones:**
1. En Home, toca "Ver historial"
2. Se abre HistorialTurnosScreen
3. Deberías ver todos tus turnos anteriores

**Qué validar:**
- ✅ Aparecen todos los turnos del estudiante
- ✅ Ordenados por fecha (más recientes primero)
- ✅ Aparecen código, tipo, estado, tiempos
- ✅ Los estados tienen colores diferenciados

**Probar Filtros:**
1. Toca chip "Completados" → solo ATENDIDO
2. Toca chip "Cancelados" → solo CANCELADO
3. Toca chip "Todos" → muestra todos nuevamente

**Qué validar:**
- ✅ Filtros funcionan correctamente
- ✅ Lista se actualiza al cambiar filtro
- ✅ Contador y etiquetas correctos

---

## 🧪 Casos de Prueba Específicos

### Caso 1: Sin Turnos
```
Expected: "Sin historial - Aún no tienes turnos registrados"
Test: ✅
```

### Caso 2: Turno con Estado ATENDIDO
```
Debería mostrar:
- ✅ Icono verde
- ✅ Fecha de solicitud y atención
- ✅ Duración real del trámite
```

### Caso 3: Turno Cancelado por Estudiante
```
Debería mostrar:
- ❌ Icono rojo
- ⚠️ "CANCELADO" 
- ⏱️ Hora de solicitud
```

### Caso 4: Múltiples Turnos en un Día
```
Expected: 
- Todos aparecen en historial
- Posiciones en fila son secuenciales (1, 2, 3...)
- Tiempos de espera se calculan correctamente
```

---

## 🔗 URLs del Backend para Testing Manual

Si quieres probar los endpoints directamente:

### Listar turnos del estudiante:
```
GET http://localhost/EduCore/backend/Turnos.php?action=listByEstudiante&estudianteId=1
```
Response esperado:
```json
{
  "success": true,
  "message": "Turnos del estudiante obtenidos correctamente.",
  "data": [
    {
      "id": 1,
      "codigo_turno": "T-001",
      "estado": "EN_COLA",
      "tipo_tramite_nombre": "Matrícula",
      "hora_solicitud": "2025-11-23 14:30:00",
      ...
    }
  ]
}
```

### Crear turno:
```
POST http://localhost/EduCore/backend/Turnos.php?action=create
Content-Type: application/json

{
  "estudiante_id": 1,
  "tipo_tramite_id": 2
}
```

### Obtener tiempo estimado:
```
GET http://localhost/EduCore/backend/Turnos.php?action=estimateTime&tipoTramiteId=2
```
Response:
```json
{
  "success": true,
  "message": "Tiempo estimado calculado.",
  "data": 45
}
```

---

## 🐛 Debugging

### Si los turnos no cargan:
1. Revisa Logcat en Android Studio
2. Verifica que BuildConfig.TURNOS_URL sea correcto
3. Comprueba conectividad de red (usa emulador con Android Studio)
4. Valida que el endpoint existe en tu servidor

### Si el tiempo estimado es 0:
1. Verifica que haya tipos de trámite activos
2. Revisa que `duracion_estimada_min` sea > 0

### Si no aparecen turnos en historial:
1. Crea al menos un turno primero
2. Revisa el ID del estudiante es correcto
3. Comprueba base de datos: `SELECT * FROM turnos WHERE estudiante_id = X;`

---

## 📊 Datos de Prueba Recomendados

Tipos de trámite a crear:
```
1. Matrícula
   - Descripción: Registro de inscripción
   - Duración: 10 min

2. Pago de Matrícula
   - Descripción: Tramitar pago de cuotas
   - Duración: 5 min

3. Constancia de Estudio
   - Descripción: Solicitar documento oficial
   - Duración: 15 min

4. Cambio de Plan
   - Descripción: Cambiar plan de estudio
   - Duración: 20 min
```

---

## ✅ Checklist de Validación

- [ ] SolicitarTurnoScreen carga tipos de trámite
- [ ] Tiempo estimado se calcula correctamente
- [ ] Turno se crea con código único
- [ ] DetalleTurnoScreen muestra información completa
- [ ] Posición en fila es correcta
- [ ] Notificación aparece cuando faltan 2 turnos
- [ ] Cancelación funciona con confirmación
- [ ] HistorialTurnosScreen lista todos los turnos
- [ ] Filtros funcionan correctamente
- [ ] Estados se muestran con colores diferenciados
- [ ] Navegación entre pantallas es fluida
- [ ] No hay crashes al cambiar de pantalla

---

## 🎉 Success Criteria

Todos estos puntos deben cumplirse para validar que el módulo funciona:

1. ✅ Crear turno sin errores
2. ✅ Ver posición en fila
3. ✅ Cancelar turno con confirmación
4. ✅ Historial poblado después de crear turnos
5. ✅ Filtros funcionando
6. ✅ UI responsive en diferentes tamaños
7. ✅ No hay crashes durante la navegación

---

**¡Listo para probar! 🚀**

