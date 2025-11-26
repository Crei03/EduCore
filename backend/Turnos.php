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
        case 'listByEstudiante':
            handleListByEstudiante();
            break;
        case 'getCurrent':
            handleGetCurrent();
            break;
        case 'create':
            requireMethod(['POST']);
            handleCreate($payload);
            break;
        case 'updateStatus':
            requireMethod(['POST', 'PUT', 'PATCH']);
            handleUpdateStatus($payload);
            break;
        case 'estimateTime':
            handleEstimateTime();
            break;
        case 'getPosition':
            handleGetPosition();
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

function requireMethod(array $allowed): void {
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

/**
 * Lista todos los turnos de un día o período específico.
 * Parámetros opcionales:
 * - fecha: YYYY-MM-DD (defecto: hoy)
 * - estado: EN_COLA, ATENDIENDO, ATENDIDO, CANCELADO, AUSENTE
 */
function handleList(): void {
    $fecha = date('Y-m-d', strtotime($_GET['fecha'] ?? 'today'));
    $estado = strtoupper((string)($_GET['estado'] ?? ''));

    $conn = createConnection();
    $query = "SELECT
        t.id, t.codigo_turno, t.estudiante_id, t.tipo_tramite_id,
        t.estado, t.hora_solicitud, t.hora_inicio_atencion, t.hora_fin_atencion,
        t.observaciones, t.creado_en, t.actualizado_en,
        tt.nombre as tipo_tramite_nombre, u.nombre as estudiante_nombre, u.apellido as estudiante_apellido
        FROM turnos t
        LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id
        LEFT JOIN usuarios u ON t.estudiante_id = u.id
        WHERE DATE(t.hora_solicitud) = ?";

    $params = [$fecha];
    $types = 's';

    if (!empty($estado)) {
        $query .= " AND t.estado = ?";
        $params[] = $estado;
        $types .= 's';
    }

    $query .= " ORDER BY t.hora_solicitud ASC";

    $stmt = $conn->prepare($query);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $result = $stmt->get_result();

    $data = [];
    while ($row = $result->fetch_assoc()) {
        $data[] = normalizeTurno($row);
    }
    $stmt->close();
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turnos obtenidos correctamente.',
        'data' => $data
    ]);
}

/**
 * Lista los turnos de un estudiante específico.
 * Parámetros requeridos:
 * - estudianteId: ID del estudiante
 * Parámetros opcionales:
 * - estado: filtrar por estado
 */
function handleListByEstudiante(): void {
    $estudianteId = (int)($_GET['estudianteId'] ?? 0);
    $estado = strtoupper((string)($_GET['estado'] ?? ''));

    if ($estudianteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de estudiante inválido.'
        ]);
        return;
    }

    $conn = createConnection();
    $query = "SELECT
        t.id, t.codigo_turno, t.estudiante_id, t.tipo_tramite_id,
        t.estado, t.hora_solicitud, t.hora_inicio_atencion, t.hora_fin_atencion,
        t.observaciones, t.creado_en, t.actualizado_en,
        tt.nombre as tipo_tramite_nombre, tt.duracion_estimada_min
        FROM turnos t
        LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id
        WHERE t.estudiante_id = ?";

    $params = [$estudianteId];
    $types = 'i';

    if (!empty($estado)) {
        $query .= " AND t.estado = ?";
        $params[] = $estado;
        $types .= 's';
    }

    $query .= " ORDER BY t.hora_solicitud DESC";

    $stmt = $conn->prepare($query);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $result = $stmt->get_result();

    $data = [];
    while ($row = $result->fetch_assoc()) {
        $data[] = normalizeTurno($row);
    }
    $stmt->close();
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turnos del estudiante obtenidos correctamente.',
        'data' => $data
    ]);
}

/**
 * Obtiene el turno actual del estudiante (EN_COLA o ATENDIENDO).
 * Parámetros requeridos:
 * - estudianteId: ID del estudiante
 */
function handleGetCurrent(): void {
    $estudianteId = (int)($_GET['estudianteId'] ?? 0);

    if ($estudianteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de estudiante inválido.'
        ]);
        return;
    }

    $conn = createConnection();
    $stmt = $conn->prepare("
        SELECT
            t.id, t.codigo_turno, t.estudiante_id, t.tipo_tramite_id,
            t.estado, t.hora_solicitud, t.hora_inicio_atencion, t.hora_fin_atencion,
            t.observaciones, t.creado_en, t.actualizado_en,
            tt.nombre as tipo_tramite_nombre, tt.duracion_estimada_min
        FROM turnos t
        LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id
        WHERE t.estudiante_id = ? AND t.estado IN ('EN_COLA', 'ATENDIENDO')
        ORDER BY t.hora_solicitud ASC
        LIMIT 1
    ");
    $stmt->bind_param('i', $estudianteId);
    $stmt->execute();
    $result = $stmt->get_result();
    $turno = $result->fetch_assoc();
    $stmt->close();

    $conn->close();

    if (!$turno) {
        echo json_encode([
            'success' => true,
            'message' => 'No hay turno actual.',
            'data' => null
        ]);
        return;
    }

    echo json_encode([
        'success' => true,
        'message' => 'Turno actual obtenido.',
        'data' => normalizeTurno($turno)
    ]);
}

/**
 * Crea un nuevo turno.
 * Parámetros requeridos (JSON):
 * - estudiante_id: ID del estudiante
 * - tipo_tramite_id: ID del tipo de trámite
 */
function handleCreate(array $payload): void {
    $estudianteId = (int)($payload['estudiante_id'] ?? 0);
    $tipoTramiteId = (int)($payload['tipo_tramite_id'] ?? 0);

    if ($estudianteId <= 0 || $tipoTramiteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Datos del turno inválidos.'
        ]);
        return;
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
    if (!tipoTramiteExists($conn, $tipoTramiteId)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El tipo de trámite no existe.'
        ]);
        return;
    }

    // Generar código de turno único (ej: T-001, T-002)
    $codigoTurno = generateCodigoTurno($conn);

    $stmt = $conn->prepare(
        "INSERT INTO turnos (codigo_turno, estudiante_id, tipo_tramite_id, estado, hora_solicitud)
         VALUES (?, ?, ?, 'EN_COLA', NOW())"
    );
    $stmt->bind_param('sii', $codigoTurno, $estudianteId, $tipoTramiteId);
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

/**
 * Actualiza el estado de un turno.
 * Parámetros requeridos (JSON):
 * - id: ID del turno
 * - estado: EN_COLA, ATENDIENDO, ATENDIDO, CANCELADO, AUSENTE
 */
function handleUpdateStatus(array $payload): void {
    $id = (int)($payload['id'] ?? 0);
    $estado = strtoupper((string)($payload['estado'] ?? ''));

    $estadosValidos = ['EN_COLA', 'ATENDIENDO', 'ATENDIDO', 'CANCELADO', 'AUSENTE'];

    if ($id <= 0 || !in_array($estado, $estadosValidos, true)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Datos inválidos.'
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

    // Actualizar según el estado
    $query = "UPDATE turnos SET estado = ?, actualizado_en = CURRENT_TIMESTAMP()";
    $params = [$estado];
    $types = 's';

    // Si cambia a ATENDIENDO, registrar hora de inicio
    if ($estado === 'ATENDIENDO') {
        $query .= ", hora_inicio_atencion = NOW()";
    }

    // Si cambia a ATENDIDO, registrar hora de fin
    if ($estado === 'ATENDIDO') {
        $query .= ", hora_fin_atencion = NOW()";
    }

    $query .= " WHERE id = ?";
    $params[] = $id;
    $types .= 'i';

    $stmt = $conn->prepare($query);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoById($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Estado actualizado correctamente.',
        'data' => $turno
    ]);
}

/**
 * Calcula el tiempo estimado de espera para un tipo de trámite.
 * Parámetros requeridos:
 * - tipoTramiteId: ID del tipo de trámite
 */
function handleEstimateTime(): void {
    $tipoTramiteId = (int)($_GET['tipoTramiteId'] ?? 0);

    if ($tipoTramiteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de tipo de trámite inválido.'
        ]);
        return;
    }

    $conn = createConnection();

    // Obtener duración estimada del tipo de trámite
    $stmt = $conn->prepare("SELECT duracion_estimada_min FROM tipos_tramite WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $tipoTramiteId);
    $stmt->execute();
    $result = $stmt->get_result();
    $tramite = $result->fetch_assoc();
    $stmt->close();

    if (!$tramite) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'Tipo de trámite no encontrado.'
        ]);
        return;
    }

    $duracionMin = (int)$tramite['duracion_estimada_min'];

    // Contar turnos en cola para ese tipo de trámite hoy
    $stmt = $conn->prepare("
        SELECT COUNT(*) as total FROM turnos
        WHERE tipo_tramite_id = ? AND estado = 'EN_COLA' AND DATE(hora_solicitud) = CURDATE()
    ");
    $stmt->bind_param('i', $tipoTramiteId);
    $stmt->execute();
    $result = $stmt->get_result();
    $count = $result->fetch_assoc();
    $stmt->close();

    $turnosEnCola = (int)$count['total'];
    $tiempoEstimado = $turnosEnCola * $duracionMin;

    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Tiempo estimado calculado.',
        'data' => $tiempoEstimado
    ]);
}

/**
 * Obtiene la posición de un turno en la fila.
 * Parámetros requeridos:
 * - turnoId: ID del turno
 */
function handleGetPosition(): void {
    $turnoId = (int)($_GET['turnoId'] ?? 0);

    if ($turnoId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'ID de turno inválido.'
        ]);
        return;
    }

    $conn = createConnection();

    // Obtener el turno para obtener su tipo de trámite
    $stmt = $conn->prepare("SELECT tipo_tramite_id FROM turnos WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $turnoId);
    $stmt->execute();
    $result = $stmt->get_result();
    $turno = $result->fetch_assoc();
    $stmt->close();

    if (!$turno) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'Turno no encontrado.'
        ]);
        return;
    }

    $tipoTramiteId = (int)$turno['tipo_tramite_id'];

    // Contar cuántos turnos EN_COLA hay antes de este
    $stmt = $conn->prepare("
        SELECT COUNT(*) as position FROM turnos
        WHERE tipo_tramite_id = ? AND estado = 'EN_COLA'
        AND hora_solicitud < (SELECT hora_solicitud FROM turnos WHERE id = ?)
    ");
    $stmt->bind_param('ii', $tipoTramiteId, $turnoId);
    $stmt->execute();
    $result = $stmt->get_result();
    $posicion = $result->fetch_assoc();
    $stmt->close();

    $conn->close();

    // La posición es el conteo + 1
    $position = ((int)$posicion['position']) + 1;

    echo json_encode([
        'success' => true,
        'message' => 'Posición obtenida.',
        'position' => $position
    ]);
}

// ============= FUNCIONES AUXILIARES =============

function normalizeTurno(array $row): array {
    return [
        'id' => (int)$row['id'],
        'codigo_turno' => (string)$row['codigo_turno'],
        'estudiante_id' => (int)$row['estudiante_id'],
        'tipo_tramite_id' => (int)$row['tipo_tramite_id'],
        'estado' => (string)$row['estado'],
        'hora_solicitud' => (string)($row['hora_solicitud'] ?? ''),
        'hora_inicio_atencion' => (string)($row['hora_inicio_atencion'] ?? ''),
        'hora_fin_atencion' => (string)($row['hora_fin_atencion'] ?? ''),
        'observaciones' => (string)($row['observaciones'] ?? ''),
        'creado_en' => (string)($row['creado_en'] ?? ''),
        'actualizado_en' => (string)($row['actualizado_en'] ?? ''),
        'tipo_tramite_nombre' => (string)($row['tipo_tramite_nombre'] ?? ''),
        'duracion_estimada_min' => isset($row['duracion_estimada_min']) ? (int)$row['duracion_estimada_min'] : null
    ];
}

function generateCodigoTurno(mysqli $conn): string {
    // Obtener el último código y generar el siguiente
    $result = $conn->query("SELECT codigo_turno FROM turnos ORDER BY id DESC LIMIT 1");
    $lastTurno = $result->fetch_assoc();
    $result->close();

    if (!$lastTurno) {
        return 'T-001';
    }

    $lastCode = $lastTurno['codigo_turno'];
    $numero = (int)substr($lastCode, 2);
    $nuevoNumero = $numero + 1;

    return 'T-' . str_pad($nuevoNumero, 3, '0', STR_PAD_LEFT);
}

function usuarioExists(mysqli $conn, int $id): bool {
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->store_result();
    $exists = $stmt->num_rows > 0;
    $stmt->close();
    return $exists;
}

function tipoTramiteExists(mysqli $conn, int $id): bool {
    $stmt = $conn->prepare("SELECT id FROM tipos_tramite WHERE id = ? AND activo = 1 LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->store_result();
    $exists = $stmt->num_rows > 0;
    $stmt->close();
    return $exists;
}

function turnoExists(mysqli $conn, int $id): bool {
    $stmt = $conn->prepare("SELECT id FROM turnos WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->store_result();
    $exists = $stmt->num_rows > 0;
    $stmt->close();
    return $exists;
}

function fetchTurnoById(mysqli $conn, int $id): array {
    $stmt = $conn->prepare("
        SELECT
            t.id, t.codigo_turno, t.estudiante_id, t.tipo_tramite_id,
            t.estado, t.hora_solicitud, t.hora_inicio_atencion, t.hora_fin_atencion,
            t.observaciones, t.creado_en, t.actualizado_en,
            tt.nombre as tipo_tramite_nombre, tt.duracion_estimada_min
        FROM turnos t
        LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id
        WHERE t.id = ? LIMIT 1
    ");
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
?>

