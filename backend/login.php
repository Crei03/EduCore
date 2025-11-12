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

$email = filter_var($payload['email'] ?? '', FILTER_VALIDATE_EMAIL);
$password = (string)($payload['password'] ?? '');

if (!$email || strlen($password) < 6) {
    http_response_code(422);
    echo json_encode([
        "success" => false,
        "message" => "Correo o contraseña inválidos."
    ]);
    exit();
}

try {
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
        exit();
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
        exit();
    }

    unset($user['password']);
    echo json_encode([
        "success" => true,
        "message" => "Bienvenido " . $user['nombre'],
        "user" => $user
    ]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error del servidor: " . $e->getMessage()
    ]);
}
