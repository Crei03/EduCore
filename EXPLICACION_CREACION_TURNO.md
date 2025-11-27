# 📋 Explicación: Creación de Turno

## ¿Por qué solo enviamos 2 campos?

### Campos que enviamos desde la APP 📱
```json
{
  "estudiante_id": 3,
  "tipo_tramite_id": 1
}
```

### ¿Qué hace el BACKEND automáticamente? 🖥️

Cuando recibes estos 2 campos, el backend PHP hace lo siguiente:

1. **Valida** que el estudiante existe en la base de datos
2. **Valida** que el tipo de trámite existe
3. **Genera automáticamente**:
   - `codigo_turno`: Genera un código único (T-001, T-002, etc.)
   - `estado`: Lo fija como `'EN_COLA'`
   - `hora_solicitud`: Usa `NOW()` (fecha/hora actual del servidor)
   
4. **Deja en NULL** (hasta que la secretaría actualice):
   - `hora_inicio_atencion`: NULL
   - `hora_fin_atencion`: NULL
   - `observaciones`: NULL

### Código del Backend (Turnos.php - línea 295)
```php
$stmt = $conn->prepare(
    "INSERT INTO turnos (codigo_turno, estudiante_id, tipo_tramite_id, estado, hora_solicitud)
     VALUES (?, ?, ?, 'EN_COLA', NOW())"
);
$stmt->bind_param('sii', $codigoTurno, $estudianteId, $tipoTramiteId);
```

## ❌ Lo que NO debes hacer

**NO envíes estos campos desde la app:**
- ❌ `estado` - El backend lo fija
- ❌ `hora_solicitud` - El backend usa NOW()
- ❌ `hora_inicio_atencion` - Se actualiza después
- ❌ `hora_fin_atencion` - Se actualiza después
- ❌ `observaciones` - Se agregan después

## 🔍 ¿Por qué te confundiste?

Cuando probaste en **Postman** enviaste todos los campos, pero el backend **ignora** los campos extra que no necesita. Solo usa `estudiante_id` y `tipo_tramite_id`.

## 🐛 Si el turno no se crea

El error NO es por campos faltantes. Revisa:

1. ✅ **Servidor corriendo**: XAMPP/Apache debe estar activo
2. ✅ **Base de datos**: MySQL debe estar corriendo
3. ✅ **URL correcta**: Verifica que la URL en `build.gradle.kts` sea correcta
4. ✅ **Datos válidos**: El `estudiante_id` y `tipo_tramite_id` deben existir en la BD
5. ✅ **Puerto**: Si usas puerto diferente al 80, actualiza las URLs

## 🔧 URLs actuales (build.gradle.kts)

```kotlin
"http://10.0.2.2:80/EduCore/backend/Turnos.php"
```

- `10.0.2.2` = localhost en Android Emulator
- `:80` = Puerto HTTP por defecto
- Si tu servidor usa otro puerto (ej: 8080), cámbialo a `:8080`

## 📊 Flujo completo

```
APP (Estudiante)
    ↓ Envía: estudiante_id, tipo_tramite_id
BACKEND (PHP)
    ↓ Valida datos
    ↓ Genera: codigo_turno, estado='EN_COLA', hora_solicitud=NOW()
    ↓ Inserta en BD
    ↓ Retorna turno completo
APP
    ↓ Recibe turno con todos los campos (incluyendo los generados)
    ↓ Navega a pantalla de detalle
```

## ✅ Conclusión

**El código actual es CORRECTO**. Solo necesitas 2 campos. Si hay error, es por conectividad o configuración, no por campos faltantes.

