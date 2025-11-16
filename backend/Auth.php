<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        "success" => false,
        "message" => "Método no permitido. Usa POST."
    ]);
    exit();
}

$action = strtolower((string)($_GET['action'] ?? ''));
if ($action === '') {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "Acción no especificada."
    ]);
    exit();
}

$rawInput = file_get_contents('php://input');
$payload = json_decode($rawInput, true);

if (!is_array($payload)) {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "Cuerpo JSON inválido."
    ]);
    exit();
}

try {
    switch ($action) {
        case 'login':
            handleLogin($payload);
            break;
        case 'register':
            handleRegister($payload);
            break;
        default:
            http_response_code(400);
            echo json_encode([
                "success" => false,
                "message" => "Acción no soportada."
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

function handleLogin(array $payload): void
{
    $email = filter_var($payload['email'] ?? '', FILTER_VALIDATE_EMAIL);
    $password = (string)($payload['password'] ?? '');

    if (!$email || strlen($password) < 6) {
        http_response_code(422);
        echo json_encode([
            "success" => false,
            "message" => "Correo o contraseña inválidos."
        ]);
        return;
    }

    $conn = createConnection();
    $stmt = $conn->prepare("SELECT id, nombre, apellido, email, password, rol FROM usuarios WHERE email = ? LIMIT 1");
    $stmt->bind_param('s', $email);
    $stmt->execute();
    $result = $stmt->get_result();
    $user = $result->fetch_assoc();
    $stmt->close();
    $conn->close();

    if (!$user) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Credenciales inválidas."
        ]);
        return;
    }

    $storedPassword = (string)$user['password'];
    $passwordMatches = password_verify($password, $storedPassword)
        || hash_equals($storedPassword, $password);

    if (!$passwordMatches) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Credenciales inválidas."
        ]);
        return;
    }

    unset($user['password']);
    echo json_encode([
        "success" => true,
        "message" => "Bienvenido " . $user['nombre'],
        "user" => $user
    ]);
}

function handleRegister(array $payload): void
{
    $nombre = trim((string)($payload['nombre'] ?? ''));
    $apellido = trim((string)($payload['apellido'] ?? ''));
    $email = filter_var($payload['email'] ?? '', FILTER_VALIDATE_EMAIL);
    $password = (string)($payload['password'] ?? '');

    if (!$email || mb_strlen($nombre) < 2 || mb_strlen($apellido) < 2) {
        http_response_code(422);
        echo json_encode([
            "success" => false,
            "message" => "Datos del usuario inválidos."
        ]);
        return;
    }

    if (strlen($password) < 8) {
        http_response_code(422);
        echo json_encode([
            "success" => false,
            "message" => "La contraseña debe tener al menos 8 caracteres."
        ]);
        return;
    }

    $conn = createConnection();

    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE email = ? LIMIT 1");
    $stmt->bind_param('s', $email);
    $stmt->execute();
    $stmt->store_result();
    $userExists = $stmt->num_rows > 0;
    $stmt->close();

    if ($userExists) {
        $conn->close();
        http_response_code(409);
        echo json_encode([
            "success" => false,
            "message" => "El correo ya está registrado."
        ]);
        return;
    }

    $hashedPassword = password_hash($password, PASSWORD_BCRYPT);
    $insert = $conn->prepare(
        "INSERT INTO usuarios (nombre, apellido, email, password) VALUES (?, ?, ?, ?)"
    );
    $insert->bind_param('ssss', $nombre, $apellido, $email, $hashedPassword);
    $insert->execute();
    $newUserId = $insert->insert_id;
    $insert->close();

    $select = $conn->prepare(
        "SELECT id, nombre, apellido, email, rol FROM usuarios WHERE id = ? LIMIT 1"
    );
    $select->bind_param('i', $newUserId);
    $select->execute();
    $result = $select->get_result();
    $user = $result->fetch_assoc();
    $select->close();
    $conn->close();

    http_response_code(201);
    echo json_encode([
        "success" => true,
        "message" => "Cuenta creada correctamente.",
        "user" => $user
    ]);
}
