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
        case 'status':
            requireMethod(['POST', 'PATCH']);
            handleStatus($payload);
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
    $includeDeleted = filter_var($_GET['includeDeleted'] ?? false, FILTER_VALIDATE_BOOLEAN);
    $conn = createConnection();
    $query = "SELECT id, nombre, descripcion, duracion_estimada_min, activo, creado_en, actualizado_en FROM tipos_tramite";
    if (!$includeDeleted) {
        $query .= " WHERE activo <> 2";
    }
    $query .= " ORDER BY actualizado_en DESC, nombre ASC";

    $result = $conn->query($query);
    $data = [];
    while ($row = $result->fetch_assoc()) {
        $data[] = normalizeTramite($row);
    }
    $result->close();
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Trámites obtenidos correctamente.',
        'data' => $data
    ]);
}

function handleCreate(array $payload): void
{
    $nombre = trim((string)($payload['nombre'] ?? ''));
    $descripcion = trim((string)($payload['descripcion'] ?? ''));
    $duracion = (int)($payload['duracion_estimada_min'] ?? 0);

    if (mb_strlen($nombre) < 3 || $duracion <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Nombre o duración inválidos.'
        ]);
        return;
    }

    $conn = createConnection();
    $stmt = $conn->prepare("INSERT INTO tipos_tramite (nombre, descripcion, duracion_estimada_min, activo) VALUES (?, ?, ?, 1)");
    $stmt->bind_param('ssi', $nombre, $descripcion, $duracion);
    $stmt->execute();
    $newId = (int)$stmt->insert_id;
    $stmt->close();

    $tramite = fetchTramiteById($conn, $newId);
    $conn->close();

    http_response_code(201);
    echo json_encode([
        'success' => true,
        'message' => 'Trámite creado correctamente.',
        'data' => $tramite
    ]);
}

function handleUpdate(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
    $nombre = trim((string)($payload['nombre'] ?? ''));
    $descripcion = trim((string)($payload['descripcion'] ?? ''));
    $duracion = (int)($payload['duracion_estimada_min'] ?? 0);

    if ($id <= 0 || mb_strlen($nombre) < 3 || $duracion <= 0) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Datos del trámite inválidos.'
        ]);
        return;
    }

    $conn = createConnection();
    if (!tramiteExists($conn, $id)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El trámite no existe.'
        ]);
        return;
    }

    $stmt = $conn->prepare("UPDATE tipos_tramite SET nombre = ?, descripcion = ?, duracion_estimada_min = ?, actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
    $stmt->bind_param('ssii', $nombre, $descripcion, $duracion, $id);
    $stmt->execute();
    $stmt->close();

    $tramite = fetchTramiteById($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Trámite actualizado correctamente.',
        'data' => $tramite
    ]);
}

function handleStatus(array $payload): void
{
    $id = (int)($payload['id'] ?? 0);
    $estado = (int)($payload['estado'] ?? -1);

    if ($id <= 0 || !in_array($estado, [0, 1, 2], true)) {
        http_response_code(422);
        echo json_encode([
            'success' => false,
            'message' => 'Parámetros inválidos.'
        ]);
        return;
    }

    $conn = createConnection();
    if (!tramiteExists($conn, $id)) {
        $conn->close();
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'message' => 'El trámite no existe.'
        ]);
        return;
    }

    $stmt = $conn->prepare("UPDATE tipos_tramite SET activo = ?, actualizado_en = CURRENT_TIMESTAMP() WHERE id = ?");
    $stmt->bind_param('ii', $estado, $id);
    $stmt->execute();
    $stmt->close();

    $tramite = fetchTramiteById($conn, $id);
    $conn->close();

    echo json_encode([
        'success' => true,
        'message' => 'Estado actualizado correctamente.',
        'data' => $tramite
    ]);
}

function normalizeTramite(array $row): array
{
    return [
        'id' => (int)$row['id'],
        'nombre' => (string)$row['nombre'],
        'descripcion' => (string)($row['descripcion'] ?? ''),
        'duracion_estimada_min' => (int)$row['duracion_estimada_min'],
        'activo' => (int)$row['activo'],
        'creado_en' => (string)($row['creado_en'] ?? ''),
        'actualizado_en' => (string)($row['actualizado_en'] ?? '')
    ];
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

function fetchTramiteById(mysqli $conn, int $id): array
{
    $stmt = $conn->prepare("SELECT id, nombre, descripcion, duracion_estimada_min, activo, creado_en, actualizado_en FROM tipos_tramite WHERE id = ? LIMIT 1");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $result = $stmt->get_result();
    $tramite = $result->fetch_assoc();
    $stmt->close();
    if (!$tramite) {
        return [];
    }
    return normalizeTramite($tramite);
}
?>
