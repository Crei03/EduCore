<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

// Habilitar CORS para desarrollo local
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Manejar preflight request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];

try {
    switch ($method) {
        case 'GET':
            handleGet();
            break;
        case 'POST':
            handlePost();
            break;
        case 'PUT':
            handlePut();
            break;
        case 'DELETE':
            handleDelete();
            break;
        default:
            http_response_code(405);
            echo json_encode([
                "success" => false,
                "message" => "Método no permitido."
            ]);
            break;
    }
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error del servidor: " . $e->getMessage()
    ]);
}

/**
 * Obtener todos los secretarios o uno específico por ID
 */
function handleGet(): void
{
    $conn = createConnection();
    
    $id = isset($_GET['id']) ? (int)$_GET['id'] : null;
    
    if ($id) {
        // Obtener un secretario específico
        $stmt = $conn->prepare("SELECT id, nombre, apellido, email, creado_en, actualizado_en FROM usuarios WHERE id = ? AND rol = 'SECRETARIA'");
        $stmt->bind_param('i', $id);
        $stmt->execute();
        $result = $stmt->get_result();
        $secretary = $result->fetch_assoc();
        $stmt->close();
        $conn->close();
        
        if ($secretary) {
            echo json_encode([
                "success" => true,
                "data" => $secretary
            ]);
        } else {
            http_response_code(404);
            echo json_encode([
                "success" => false,
                "message" => "Secretario no encontrado."
            ]);
        }
    } else {
        // Obtener todos los secretarios
        $result = $conn->query("SELECT id, nombre, apellido, email, creado_en, actualizado_en FROM usuarios WHERE rol = 'SECRETARIA' ORDER BY creado_en DESC");
        $secretaries = [];
        
        while ($row = $result->fetch_assoc()) {
            $secretaries[] = $row;
        }
        
        $conn->close();
        
        echo json_encode([
            "success" => true,
            "data" => $secretaries
        ]);
    }
}

/**
 * Crear un nuevo secretario
 */
function handlePost(): void
{
    $rawInput = file_get_contents('php://input');
    $payload = json_decode($rawInput, true);
    
    if (!is_array($payload)) {
        http_response_code(400);
        echo json_encode([
            "success" => false,
            "message" => "Cuerpo JSON inválido."
        ]);
        return;
    }
    
    // Validar campos requeridos
    $nombre = trim((string)($payload['nombre'] ?? ''));
    $apellido = trim((string)($payload['apellido'] ?? ''));
    $email = filter_var($payload['email'] ?? '', FILTER_VALIDATE_EMAIL);
    $password = (string)($payload['password'] ?? '');
    
    $errors = [];
    
    if (empty($nombre)) {
        $errors[] = "El nombre es requerido.";
    } elseif (strlen($nombre) > 80) {
        $errors[] = "El nombre no puede exceder 80 caracteres.";
    }
    
    if (empty($apellido)) {
        $errors[] = "El apellido es requerido.";
    } elseif (strlen($apellido) > 80) {
        $errors[] = "El apellido no puede exceder 80 caracteres.";
    }
    
    if (!$email) {
        $errors[] = "Correo electrónico inválido.";
    } elseif (strlen($email) > 120) {
        $errors[] = "El correo no puede exceder 120 caracteres.";
    }
    
    if (strlen($password) < 6) {
        $errors[] = "La contraseña debe tener al menos 6 caracteres.";
    }
    
    if (!empty($errors)) {
        http_response_code(422);
        echo json_encode([
            "success" => false,
            "message" => implode(" ", $errors)
        ]);
        return;
    }
    
    $conn = createConnection();
    
    // Verificar si el correo ya existe
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE email = ?");
    $stmt->bind_param('s', $email);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        $stmt->close();
        $conn->close();
        http_response_code(409);
        echo json_encode([
            "success" => false,
            "message" => "El correo electrónico ya está registrado."
        ]);
        return;
    }
    $stmt->close();
    
    // Insertar nuevo secretario
    $hashedPassword = password_hash($password, PASSWORD_DEFAULT);
    $rol = 'SECRETARIA';
    
    $stmt = $conn->prepare("INSERT INTO usuarios (nombre, apellido, email, password, rol) VALUES (?, ?, ?, ?, ?)");
    $stmt->bind_param('sssss', $nombre, $apellido, $email, $hashedPassword, $rol);
    
    if ($stmt->execute()) {
        $newId = $conn->insert_id;
        $stmt->close();
        $conn->close();
        
        http_response_code(201);
        echo json_encode([
            "success" => true,
            "message" => "Secretario creado exitosamente.",
            "data" => ["id" => $newId]
        ]);
    } else {
        $stmt->close();
        $conn->close();
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "message" => "Error al crear el secretario."
        ]);
    }
}

/**
 * Actualizar un secretario existente
 */
function handlePut(): void
{
    $id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
    
    if ($id <= 0) {
        http_response_code(400);
        echo json_encode([
            "success" => false,
            "message" => "ID de secretario no especificado."
        ]);
        return;
    }
    
    $rawInput = file_get_contents('php://input');
    $payload = json_decode($rawInput, true);
    
    if (!is_array($payload)) {
        http_response_code(400);
        echo json_encode([
            "success" => false,
            "message" => "Cuerpo JSON inválido."
        ]);
        return;
    }
    
    $conn = createConnection();
    
    // Verificar que el secretario existe
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE id = ? AND rol = 'SECRETARIA'");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        $stmt->close();
        $conn->close();
        http_response_code(404);
        echo json_encode([
            "success" => false,
            "message" => "Secretario no encontrado."
        ]);
        return;
    }
    $stmt->close();
    
    // Validar campos
    $nombre = trim((string)($payload['nombre'] ?? ''));
    $apellido = trim((string)($payload['apellido'] ?? ''));
    $email = filter_var($payload['email'] ?? '', FILTER_VALIDATE_EMAIL);
    $password = isset($payload['password']) ? (string)$payload['password'] : null;
    
    $errors = [];
    
    if (empty($nombre)) {
        $errors[] = "El nombre es requerido.";
    } elseif (strlen($nombre) > 80) {
        $errors[] = "El nombre no puede exceder 80 caracteres.";
    }
    
    if (empty($apellido)) {
        $errors[] = "El apellido es requerido.";
    } elseif (strlen($apellido) > 80) {
        $errors[] = "El apellido no puede exceder 80 caracteres.";
    }
    
    if (!$email) {
        $errors[] = "Correo electrónico inválido.";
    } elseif (strlen($email) > 120) {
        $errors[] = "El correo no puede exceder 120 caracteres.";
    }
    
    if ($password !== null && strlen($password) > 0 && strlen($password) < 6) {
        $errors[] = "La contraseña debe tener al menos 6 caracteres.";
    }
    
    if (!empty($errors)) {
        $conn->close();
        http_response_code(422);
        echo json_encode([
            "success" => false,
            "message" => implode(" ", $errors)
        ]);
        return;
    }
    
    // Verificar si el correo ya existe en otro usuario
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE email = ? AND id != ?");
    $stmt->bind_param('si', $email, $id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        $stmt->close();
        $conn->close();
        http_response_code(409);
        echo json_encode([
            "success" => false,
            "message" => "El correo electrónico ya está registrado por otro usuario."
        ]);
        return;
    }
    $stmt->close();
    
    // Actualizar secretario
    if ($password !== null && strlen($password) >= 6) {
        $hashedPassword = password_hash($password, PASSWORD_DEFAULT);
        $stmt = $conn->prepare("UPDATE usuarios SET nombre = ?, apellido = ?, email = ?, password = ? WHERE id = ?");
        $stmt->bind_param('ssssi', $nombre, $apellido, $email, $hashedPassword, $id);
    } else {
        $stmt = $conn->prepare("UPDATE usuarios SET nombre = ?, apellido = ?, email = ? WHERE id = ?");
        $stmt->bind_param('sssi', $nombre, $apellido, $email, $id);
    }
    
    if ($stmt->execute()) {
        $stmt->close();
        $conn->close();
        
        echo json_encode([
            "success" => true,
            "message" => "Secretario actualizado exitosamente."
        ]);
    } else {
        $stmt->close();
        $conn->close();
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "message" => "Error al actualizar el secretario."
        ]);
    }
}

/**
 * Eliminar un secretario
 */
function handleDelete(): void
{
    $id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
    
    if ($id <= 0) {
        http_response_code(400);
        echo json_encode([
            "success" => false,
            "message" => "ID de secretario no especificado."
        ]);
        return;
    }
    
    $conn = createConnection();
    
    // Verificar que el secretario existe
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE id = ? AND rol = 'SECRETARIA'");
    $stmt->bind_param('i', $id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        $stmt->close();
        $conn->close();
        http_response_code(404);
        echo json_encode([
            "success" => false,
            "message" => "Secretario no encontrado."
        ]);
        return;
    }
    $stmt->close();
    
    // Eliminar secretario
    $stmt = $conn->prepare("DELETE FROM usuarios WHERE id = ? AND rol = 'SECRETARIA'");
    $stmt->bind_param('i', $id);
    
    if ($stmt->execute()) {
        $stmt->close();
        $conn->close();
        
        echo json_encode([
            "success" => true,
            "message" => "Secretario eliminado exitosamente."
        ]);
    } else {
        $stmt->close();
        $conn->close();
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "message" => "Error al eliminar el secretario."
        ]);
    }
}
?>
