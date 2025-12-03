<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

const ESTADOS_VALIDOS = ['EN_COLA', 'ATENDIENDO', 'ATENDIDO', 'CANCELADO', 'AUSENTE'];

$action = strtolower((string)($_GET['action'] ?? ''));
if ($action === '') {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Acción no especificada.'
    ]);
    exit();
}

$payload = [];
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    $rawInput = file_get_contents('php://input');
    if ($rawInput !== '' && $rawInput !== false) {
        $decoded = json_decode($rawInput, true);
        if (is_array($decoded)) {
            $payload = $decoded;
        } elseif (trim($rawInput) !== '') {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Cuerpo JSON inválido.'
            ]);
            exit();
        }
    }
}

try {
    switch ($action) {
        case 'list':
            handleList();
            break;
        case 'listbyestudiante':
            handleListByEstudiante();
            break;
        case 'getcurrent':
            handleGetCurrent();
            break;
        case 'estimatetime':
            handleEstimateTime();
            break;
        case 'getposition':
            handleGetPosition();
            break;
        case 'updatestatus':
            requireMethod(['POST', 'PATCH']);
            handleUpdateStatus($payload);
            break;
        case 'queue':
            handleQueue();
            break;
        case 'callnext':
            requireMethod(['POST']);
            handleCallNext();
            break;
        case 'finish':
            requireMethod(['POST', 'PATCH']);
            handleFinishService($payload);
            break;
        case 'markabsent':
            requireMethod(['POST', 'PATCH']);
            handleMarkAbsent($payload);
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

function estadoValido(string $estado): bool
{
    return in_array(strtoupper($estado), ESTADOS_VALIDOS, true);
}

function handleList(): void
{
    $estudianteId = (int)($_GET['estudiante_id'] ?? ($_GET['estudianteId'] ?? 0));
    $estado = strtoupper(trim((string)($_GET['estado'] ?? '')));
    $conn = createConnection();

    $conditions = [];
    $types = '';
    $params = [];
    if ($estudianteId > 0) {
        $conditions[] = 't.estudiante_id = ?';
        $types .= 'i';
        $params[] = $estudianteId;
    }
    if ($estado !== '' && estadoValido($estado)) {
        $conditions[] = 't.estado = ?';
        $types .= 's';
        $params[] = $estado;
    }

    $where = $conditions ? 'WHERE ' . implode(' AND ', $conditions) : '';
    $turnos = fetchTurnos($conn, $where, $types, $params, false, false);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turnos obtenidos correctamente.',
        'data' => $turnos
    ]);
}

function handleListByEstudiante(): void
{
    $estudianteId = (int)($_GET['estudiante_id'] ?? ($_GET['estudianteId'] ?? 0));
    $estado = strtoupper(trim((string)($_GET['estado'] ?? '')));
    if ($estudianteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'El parámetro estudiante_id es obligatorio.'
        ]);
        return;
    }

    $conn = createConnection();
    $conditions = ['t.estudiante_id = ?'];
    $types = 'i';
    $params = [$estudianteId];
    if ($estado !== '' && estadoValido($estado)) {
        $conditions[] = 't.estado = ?';
        $types .= 's';
        $params[] = $estado;
    }
    $where = 'WHERE ' . implode(' AND ', $conditions);
    $turnos = fetchTurnos($conn, $where, $types, $params, false, false);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turnos del estudiante obtenidos correctamente.',
        'data' => $turnos
    ]);
}

function handleGetCurrent(): void
{
    $estudianteId = (int)($_GET['estudiante_id'] ?? ($_GET['estudianteId'] ?? 0));
    if ($estudianteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'El parámetro estudiante_id es obligatorio.'
        ]);
        return;
    }

    $conn = createConnection();
    $query = "SELECT t.*, tt.nombre AS tipo_tramite_nombre 
              FROM turnos t 
              LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id
              WHERE t.estudiante_id = ? AND t.estado IN ('EN_COLA','ATENDIENDO')
              ORDER BY t.hora_solicitud ASC LIMIT 1";
    $stmt = $conn->prepare($query);
    $stmt->bind_param('i', $estudianteId);
    $stmt->execute();
    $result = $stmt->get_result();
    $turno = $result->fetch_assoc();
    $stmt->close();
    $conn->close();

    if (!$turno) {
        echo json_encode([
            'success' => true,
            'message' => 'Sin turnos en curso.',
            'data' => null
        ]);
        return;
    }

    echo json_encode([
        'success' => true,
        'message' => 'Turno obtenido correctamente.',
        'data' => normalizeTurno($turno)
    ]);
}

function handleEstimateTime(): void
{
    $tipoTramiteId = (int)($_GET['tipoTramiteId'] ?? ($_GET['tipo_tramite_id'] ?? 0));
    if ($tipoTramiteId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'El parámetro tipoTramiteId es obligatorio.'
        ]);
        return;
    }

    $conn = createConnection();
    $stmt = $conn->prepare("SELECT duracion_estimada_min FROM tipos_tramite WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $tipoTramiteId);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();

    if (!$row) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El tipo de trámite no existe.'
        ]);
        return;
    }

    $duracion = (int)($row['duracion_estimada_min'] ?? 10);
    $stmt = $conn->prepare("SELECT COUNT(*) AS en_cola FROM turnos WHERE estado = 'EN_COLA' AND tipo_tramite_id = ?");
    $stmt->bind_param('i', $tipoTramiteId);
    $stmt->execute();
    $countResult = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    $conn->close();

    $enCola = (int)($countResult['en_cola'] ?? 0);
    $estimado = $enCola * $duracion;

    echo json_encode([
        'success' => true,
        'message' => 'Tiempo estimado calculado.',
        'data' => $estimado
    ]);
}

function handleGetPosition(): void
{
    $turnoId = (int)($_GET['turnoId'] ?? ($_GET['turno_id'] ?? 0));
    if ($turnoId <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'El parámetro turnoId es obligatorio.'
        ]);
        return;
    }

    $conn = createConnection();
    $stmt = $conn->prepare("SELECT hora_solicitud FROM turnos WHERE id = ? LIMIT 1");
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
            'message' => 'El turno no existe.'
        ]);
        return;
    }

    $hora = $turno['hora_solicitud'];
    $stmt = $conn->prepare("SELECT COUNT(*) AS posicion FROM turnos WHERE estado = 'EN_COLA' AND hora_solicitud <= ? AND DATE(hora_solicitud) = DATE(?)");
    $stmt->bind_param('ss', $hora, $hora);
    $stmt->execute();
    $posResult = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Posición obtenida.',
        'position' => (int)($posResult['posicion'] ?? 0)
    ]);
}

function handleQueue(): void
{
    $fecha = trim((string)($_GET['fecha'] ?? date('Y-m-d')));
    $conn = createConnection();
    $where = "WHERE DATE(t.hora_solicitud) = ? AND t.estado IN ('EN_COLA','ATENDIENDO')";
    $turnos = fetchTurnos($conn, $where, 's', [$fecha], true, true);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Cola del día obtenida correctamente.',
        'data' => $turnos
    ]);
}

function handleCallNext(): void
{
    $conn = createConnection();
    $conn->begin_transaction();
    try {
        $stmt = $conn->prepare("SELECT id FROM turnos WHERE DATE(hora_solicitud) = CURDATE() AND estado = 'EN_COLA' ORDER BY hora_solicitud ASC LIMIT 1 FOR UPDATE");
        $stmt->execute();
        $stmt->bind_result($turnoId);
        $hasTurno = $stmt->fetch();
        $stmt->close();

        if (!$hasTurno) {
            $conn->rollback();
            $conn->close();
            http_response_code(404);
            echo json_encode([
                'success' => false,
                'message' => 'No hay turnos en cola para hoy.'
            ]);
            return;
        }

        $update = $conn->prepare("UPDATE turnos SET estado = 'ATENDIENDO', hora_inicio_atencion = COALESCE(hora_inicio_atencion, NOW()), actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
        $update->bind_param('i', $turnoId);
        $update->execute();
        $update->close();

        $conn->commit();
        $turno = fetchTurnoDetallado($conn, (int)$turnoId);
        $conn->close();

        echo json_encode([
            'success' => true,
            'message' => 'Turno llamado. Se notificó al estudiante que es su turno.',
            'data' => $turno,
            'notificacion' => 'Aviso enviado al estudiante: está por ser atendido.'
        ]);
    } catch (Throwable $e) {
        $conn->rollback();
        $conn->close();
        throw $e;
    }
}

function handleFinishService(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
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

    $stmt = $conn->prepare("UPDATE turnos SET estado = 'ATENDIDO', hora_inicio_atencion = COALESCE(hora_inicio_atencion, NOW()), hora_fin_atencion = NOW(), actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoDetallado($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Atención finalizada y marcada como atendida.',
        'data' => $turno
    ]);
}

function handleMarkAbsent(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
    $motivo = trim((string)($payload['motivo'] ?? ''));
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

    $stmt = $conn->prepare("UPDATE turnos SET estado = 'CANCELADO', observaciones = ?, actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
    $stmt->bind_param('si', $motivo, $id);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoDetallado($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turno marcado como cancelado/ausente.',
        'data' => $turno
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

    if (!estadoValido($estado)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Estado inválido.'
        ]);
        return;
    }

    if (!validarFechaHora($horaSolicitud)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Formato de hora inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
        ]);
        return;
    }

    if (!empty($horaInicioAtencion) && !validarFechaHora($horaInicioAtencion)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Formato de hora_inicio_atencion inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
        ]);
        return;
    }

    if (!empty($horaFinAtencion) && !validarFechaHora($horaFinAtencion)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Formato de hora_fin_atencion inválido. Usa HH:MM, HH:MM:SS o YYYY-MM-DD HH:MM:SS.'
        ]);
        return;
    }

    $conn = createConnection();

    if (!usuarioExists($conn, $estudianteId)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El estudiante no existe.'
        ]);
        return;
    }

    if (!tramiteExists($conn, $tipoTramiteId)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El tipo de trámite no existe.'
        ]);
        return;
    }

    $turnoActivo = fetchTurnoActivo($conn, $estudianteId);
    if ($turnoActivo !== null) {
        $conn->close();
        http_response_code(409);
        echo json_encode([
            'success' => false,
            'message' => 'Ya tienes un turno en curso. Cancélalo o espera a que finalice antes de solicitar otro.',
            'data' => $turnoActivo
        ]);
        return;
    }

    $codigoTurno = generarCodigoTurno($conn);
    $horaInicioAtencionDb = $horaInicioAtencion === '' ? null : $horaInicioAtencion;
    $horaFinAtencionDb = $horaFinAtencion === '' ? null : $horaFinAtencion;
    $observacionesDb = $observaciones === '' ? null : $observaciones;

    $stmt = $conn->prepare("INSERT INTO turnos (codigo_turno, estudiante_id, tipo_tramite_id, estado, hora_solicitud, hora_inicio_atencion, hora_fin_atencion, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
    $stmt->bind_param(
        'siisssss',
        $codigoTurno,
        $estudianteId,
        $tipoTramiteId,
        $estado,
        $horaSolicitud,
        $horaInicioAtencionDb,
        $horaFinAtencionDb,
        $observacionesDb
    );
    $stmt->execute();
    $newId = (int)$stmt->insert_id;
    $stmt->close();

    $turno = fetchTurnoDetallado($conn, $newId);
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

    $updates = [];
    $types = '';
    $params = [];

    if (!empty($horaSolicitud)) {
        if (!validarFechaHora($horaSolicitud)) {
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
        if (!validarFechaHora($horaInicioAtencion)) {
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
        if (!validarFechaHora($horaFinAtencion)) {
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

    $turno = fetchTurnoDetallado($conn, $id);
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

    $estado = 'CANCELADO';
    $stmt = $conn->prepare("UPDATE turnos SET estado = ?, observaciones = ?, actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
    $stmt->bind_param('ssi', $estado, $razonCancelacion, $id);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoDetallado($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Turno cancelado correctamente.',
        'data' => $turno
    ]);
}

function handleUpdateStatus(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
    $estado = strtoupper(trim((string)($payload['estado'] ?? '')));
    $observaciones = trim((string)($payload['observaciones'] ?? ''));

    if ($id <= 0 || !estadoValido($estado)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Parámetros inválidos para actualizar el estado.'
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

    $extra = '';
    if ($estado === 'ATENDIENDO') {
        $extra = ", hora_inicio_atencion = COALESCE(hora_inicio_atencion, NOW())";
    }
    if ($estado === 'ATENDIDO') {
        $extra = ", hora_inicio_atencion = COALESCE(hora_inicio_atencion, NOW()), hora_fin_atencion = NOW()";
    }

    $stmt = $conn->prepare("UPDATE turnos SET estado = ?, observaciones = ?, actualizado_en = CURRENT_TIMESTAMP() {$extra} WHERE id = ?");
    $stmt->bind_param('ssi', $estado, $observaciones, $id);
    $stmt->execute();
    $stmt->close();

    $turno = fetchTurnoDetallado($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Estado actualizado correctamente.',
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
    $turno = fetchTurnoDetallado($conn, $id);
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

function fetchTurnos(mysqli $conn, string $where, string $types = '', array $params = [], bool $orderAsc = true, bool $incluirUsuario = false): array
{
    $select = "t.*, tt.nombre AS tipo_tramite_nombre";
    $joins = " LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id";
    if ($incluirUsuario) {
        $select .= ", u.nombre AS estudiante_nombre, u.apellido AS estudiante_apellido, u.email AS estudiante_email";
        $joins .= " LEFT JOIN usuarios u ON t.estudiante_id = u.id";
    }
    $query = "SELECT {$select} FROM turnos t {$joins} {$where} ORDER BY t.hora_solicitud " . ($orderAsc ? "ASC" : "DESC");
    $stmt = $conn->prepare($query);
    if ($types !== '') {
        $stmt->bind_param($types, ...$params);
    }
    $stmt->execute();
    $result = $stmt->get_result();
    $data = [];
    while ($row = $result->fetch_assoc()) {
        $data[] = normalizeTurno($row);
    }
    $stmt->close();
    return $data;
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
        'hora_inicio_atencion' => isset($row['hora_inicio_atencion']) && $row['hora_inicio_atencion'] !== null && $row['hora_inicio_atencion'] !== '' ? (string)$row['hora_inicio_atencion'] : null,
        'hora_fin_atencion' => isset($row['hora_fin_atencion']) && $row['hora_fin_atencion'] !== null && $row['hora_fin_atencion'] !== '' ? (string)$row['hora_fin_atencion'] : null,
        'observaciones' => (string)($row['observaciones'] ?? ''),
        'creado_en' => (string)($row['creado_en'] ?? ''),
        'actualizado_en' => (string)($row['actualizado_en'] ?? ''),
        'estudiante_nombre' => $row['estudiante_nombre'] ?? null,
        'estudiante_apellido' => $row['estudiante_apellido'] ?? null,
        'estudiante_email' => $row['estudiante_email'] ?? null,
        'tipo_tramite_nombre' => $row['tipo_tramite_nombre'] ?? null,
        'posicion_en_fila' => isset($row['posicion_en_fila']) ? (int)$row['posicion_en_fila'] : null,
        'tiempo_estimado_min' => isset($row['tiempo_estimado_min']) ? (int)$row['tiempo_estimado_min'] : null
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

function fetchTurnoDetallado(mysqli $conn, int $id): array
{
    $stmt = $conn->prepare("SELECT t.*, u.nombre AS estudiante_nombre, u.apellido AS estudiante_apellido, u.email AS estudiante_email, tt.nombre AS tipo_tramite_nombre FROM turnos t LEFT JOIN usuarios u ON t.estudiante_id = u.id LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id WHERE t.id = ? LIMIT 1");
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

function fetchTurnoActivo(mysqli $conn, int $estudianteId): ?array
{
    $query = "SELECT t.*, u.nombre AS estudiante_nombre, u.apellido AS estudiante_apellido, u.email AS estudiante_email, tt.nombre AS tipo_tramite_nombre FROM turnos t LEFT JOIN usuarios u ON t.estudiante_id = u.id LEFT JOIN tipos_tramite tt ON t.tipo_tramite_id = tt.id WHERE t.estudiante_id = ? AND t.estado IN ('EN_COLA','ATENDIENDO') ORDER BY t.hora_solicitud ASC LIMIT 1";
    $stmt = $conn->prepare($query);
    $stmt->bind_param('i', $estudianteId);
    $stmt->execute();
    $result = $stmt->get_result();
    $turno = $result->fetch_assoc();
    $stmt->close();

    if (!$turno) {
        return null;
    }

    return normalizeTurno($turno);
}

function generarCodigoTurno(mysqli $conn): string
{
    // Máximo 10 caracteres (columna varchar(10) en BD)
    $fecha = date('ymd'); // 6 caracteres
    $stmt = $conn->prepare("SELECT COUNT(*) as count FROM turnos WHERE DATE(creado_en) = CURDATE()");
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();
    
    $numeroSecuencial = (int)($row['count'] ?? 0) + 1;
    // T + fecha(6) + secuencia(3) = 10 caracteres
    $secuencia = str_pad((string)$numeroSecuencial, 3, '0', STR_PAD_LEFT);
    return 'T' . $fecha . $secuencia;
}

function validarFechaHora(string $valor): bool
{
    return preg_match('/^(\d{2}:\d{2}(:\d{2})?|\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(:\d{2})?)$/', $valor) === 1;
}
?>
