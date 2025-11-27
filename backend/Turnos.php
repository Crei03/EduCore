<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

$action = strtolower((string)($_GET['action'] ?? ''));
if ($action === '') {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Acción no especificada.'
    ]);
    exit();
}

$payload = null;
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $rawInput = file_get_contents('php://input');
    $payload = json_decode($rawInput, true);
    if (!is_array($payload)) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Cuerpo JSON inválido.'
        ]);
        exit();
    }
}

try {
    switch ($action) {
        case 'list':
            handleList();
            break;
        case 'create':
            requireMethod(['POST']);
            handleCreate($payload);
            break;
        case 'update':
            requireMethod(['POST', 'PUT']);
            handleUpdate($payload);
            break;
        case 'cancel':
            requireMethod(['POST', 'PATCH']);
            handleCancel($payload);
            break;
        case 'get':
            handleGet();
            break;
        default:
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Acción no soportada.'
            ]);
            break;
    }
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error del servidor: ' . $e->getMessage()
    ]);
}

function requireMethod(array $allowed): void
{
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
    if (!in_array($method, array_map('strtoupper', $allowed), true)) {
        http_response_code(405);
        echo json_encode([
            'success' => false,
            'message' => 'Método no permitido. Usa: ' . implode(', ', $allowed)
        ]);
        exit();
    }
}

function handleList(): void
{
    $estudianteId = (int)($_GET['estudiante_id'] ?? 0);
    $conn = createConnection();
    
    $query = "SELECT id, codigo_turno, estudiante_id, tipo_tramite_id, estado, hora_solicitud, hora_inicio_atencion, hora_fin_atencion, observaciones, creado_en, actualizado_en FROM turnos WHERE 1=1";
    
    if ($estudianteId > 0) {
        $query .= " AND estudiante_id = " . $estudianteId;
    }
    
    $query .= " ORDER BY creado_en DESC";

    $result = $conn->query($query);
    $data = [];
    while ($row = $result->fetch_assoc()) {
        $data[] = normalizeTurno($row);
    }
    $result->close();
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turnos obtenidos correctamente.',
        'data' => $data
    ]);
}

function handleCreate(array $payload): void
{
    $estudianteId = (int)($payload['estudiante_id'] ?? 0);
    $tipoTramiteId = (int)($payload['tipo_tramite_id'] ?? 0);
    $horaSolicitud = trim((string)($payload['hora_solicitud'] ?? ''));
    $estado = trim((string)($payload['estado'] ?? 'EN_COLA'));
    $horaInicioAtencion = trim((string)($payload['hora_inicio_atencion'] ?? ''));
    $horaFinAtencion = trim((string)($payload['hora_fin_atencion'] ?? ''));
    $observaciones = trim((string)($payload['observaciones'] ?? ''));

    if ($estudianteId <= 0 || $tipoTramiteId <= 0 || empty($horaSolicitud)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Datos del turno inválidos. Se requiere: estudiante_id, tipo_tramite_id, hora_solicitud.'
        ]);
        return;
    }

    // Validar formato de hora (HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS)
    if (!preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $horaSolicitud)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Formato de hora inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
        ]);
        return;
    }

    // Validar horas opcionales si se proporcionan
    if (!empty($horaInicioAtencion)) {
        if (!preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $horaInicioAtencion)) {
            http_response_code(422);
            echo json_encode([
                'success' => false,
                'message' => 'Formato de hora_inicio_atencion inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
            ]);
            return;
        }
    }

    if (!empty($horaFinAtencion)) {
        if (!preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $horaFinAtencion)) {
            http_response_code(422);
            echo json_encode([
                'success' => false,
                'message' => 'Formato de hora_fin_atencion inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
            ]);
            return;
        }
    }

    $conn = createConnection();

    // Verificar que el estudiante existe
    if (!usuarioExists($conn, $estudianteId)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El estudiante no existe.'
        ]);
        return;
    }

    // Verificar que el tipo de trámite existe
    if (!tramiteExists($conn, $tipoTramiteId)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El tipo de trámite no existe.'
        ]);
        return;
    }

    // Generar código de turno único
    $codigoTurno = generarCodigoTurno($conn);

    $stmt = $conn->prepare("INSERT INTO turnos (codigo_turno, estudiante_id, tipo_tramite_id, estado, hora_solicitud, hora_inicio_atencion, hora_fin_atencion, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
    $stmt->bind_param('siisssss', $codigoTurno, $estudianteId, $tipoTramiteId, $estado, $horaSolicitud, $horaInicioAtencion, $horaFinAtencion, $observaciones);
    $stmt->execute();
    $newId = (int)$stmt->insert_id;
    $stmt->close();

    $turno = fetchTurnoById($conn, $newId);
    $conn->close();

    http_response_code(201);
    echo json_encode([
        'success' => true,
        'message' => 'Turno creado correctamente.',
        'data' => $turno
    ]);
}

function handleUpdate(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
    $horaSolicitud = trim((string)($payload['hora_solicitud'] ?? ''));
    $horaInicioAtencion = trim((string)($payload['hora_inicio_atencion'] ?? ''));
    $horaFinAtencion = trim((string)($payload['hora_fin_atencion'] ?? ''));
    $observaciones = trim((string)($payload['observaciones'] ?? ''));

    if ($id <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de turno inválido.'
        ]);
        return;
    }

    $conn = createConnection();
    if (!turnoExists($conn, $id)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El turno no existe.'
        ]);
        return;
    }

    // Preparar actualización dinámica
    $updates = [];
    $types = '';
    $params = [];

    if (!empty($horaSolicitud)) {
        if (!preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $horaSolicitud)) {
            $conn->close();
            http_response_code(422);
            echo json_encode([
                'success' => false,
                'message' => 'Formato de hora_solicitud inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
            ]);
            return;
        }
        $updates[] = 'hora_solicitud = ?';
        $types .= 's';
        $params[] = $horaSolicitud;
    }

    if (!empty($horaInicioAtencion)) {
        if (!preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $horaInicioAtencion)) {
            $conn->close();
            http_response_code(422);
            echo json_encode([
                'success' => false,
                'message' => 'Formato de hora_inicio_atencion inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
            ]);
            return;
        }
        $updates[] = 'hora_inicio_atencion = ?';
        $types .= 's';
        $params[] = $horaInicioAtencion;
    }

    if (!empty($horaFinAtencion)) {
        if (!preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $horaFinAtencion)) {
            $conn->close();
            http_response_code(422);
            echo json_encode([
                'success' => false,
                'message' => 'Formato de hora_fin_atencion inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
            ]);
            return;
        }
        $updates[] = 'hora_fin_atencion = ?';
        $types .= 's';
        $params[] = $horaFinAtencion;
    }

    if (!empty($observaciones)) {
        $updates[] = 'observaciones = ?';
        $types .= 's';
        $params[] = $observaciones;
    }

    if (empty($updates)) {
        $conn->close();
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'No hay datos para actualizar.'
        ]);
        return;
    }

    $updates[] = 'actualizado_en = CURRENT_TIMESTAMP()';
    $params[] = $id;
    $types .= 'i';

    $query = "UPDATE turnos SET " . implode(', ', $updates) . " WHERE id = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoById($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turno actualizado correctamente.',
        'data' => $turno
    ]);
}

function handleCancel(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
    $razonCancelacion = trim((string)($payload['razon_cancelacion'] ?? ''));

    if ($id <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de turno inválido.'
        ]);
        return;
    }

    $conn = createConnection();
    if (!turnoExists($conn, $id)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El turno no existe.'
        ]);
        return;
    }

    $estado = 'cancelado';
    $stmt = $conn->prepare("UPDATE turnos SET estado = ?, observaciones = ?, actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
    $stmt->bind_param('ssi', $estado, $razonCancelacion, $id);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoById($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turno cancelado correctamente.',
        'data' => $turno
    ]);
}

function handleGet(): void
{
    $id = (int)($_GET['id'] ?? 0);

    if ($id <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de turno inválido.'
        ]);
        return;
    }

    $conn = createConnection();
    $turno = fetchTurnoById($conn, $id);
    $conn->close();

    if (empty($turno)) {
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El turno no existe.'
        ]);
        return;
    }

    echo json_encode([
        'success' => true,
        'message' => 'Turno obtenido correctamente.',
        'data' => $turno
    ]);
}

function normalizeTurno(array $row): array
{
    return [
        'id' => (int)$row['id'],
        'codigo_turno' => (string)$row['codigo_turno'],
        'estudiante_id' => (int)$row['estudiante_id'],
        'tipo_tramite_id' => (int)$row['tipo_tramite_id'],
        'estado' => (string)$row['estado'],
        'hora_solicitud' => (string)$row['hora_solicitud'],
        'hora_inicio_atencion' => (string)($row['hora_inicio_atencion'] ?? null),
        'hora_fin_atencion' => (string)($row['hora_fin_atencion'] ?? null),
        'observaciones' => (string)($row['observaciones'] ?? ''),
        'creado_en' => (string)($row['creado_en'] ?? ''),
        'actualizado_en' => (string)($row['actualizado_en'] ?? '')
    ];
}

function turnoExists(mysqli $conn, int $id): bool
{
    $stmt = $conn->prepare("SELECT id FROM turnos WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->store_result();
    $exists = $stmt->num_rows > 0;
    $stmt->close();
    return $exists;
}

function usuarioExists(mysqli $conn, int $id): bool
{
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->store_result();
    $exists = $stmt->num_rows > 0;
    $stmt->close();
    return $exists;
}

function tramiteExists(mysqli $conn, int $id): bool
{
    $stmt = $conn->prepare("SELECT id FROM tipos_tramite WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->store_result();
    $exists = $stmt->num_rows > 0;
    $stmt->close();
    return $exists;
}

function fetchTurnoById(mysqli $conn, int $id): array
{
    $stmt = $conn->prepare("SELECT id, codigo_turno, estudiante_id, tipo_tramite_id, estado, hora_solicitud, hora_inicio_atencion, hora_fin_atencion, observaciones, creado_en, actualizado_en FROM turnos WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $result = $stmt->get_result();
    $turno = $result->fetch_assoc();
    $stmt->close();
    if (!$turno) {
        return [];
    }
    return normalizeTurno($turno);
}

function generarCodigoTurno(mysqli $conn): string
{
    $fecha = date('Ymd');
    $stmt = $conn->prepare("SELECT COUNT(*) as count FROM turnos WHERE DATE(creado_en) = CURDATE()");
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();
    
    $numeroSecuencial = ($row['count'] + 1);
    return $fecha . '-' . str_pad((string)$numeroSecuencial, 4, '0', STR_PAD_LEFT);
}
?>
