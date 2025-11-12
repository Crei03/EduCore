<?php
declare(strict_types=1);

/**
 * Configuración de la base de datos.
 * Ajusta estos valores según tu entorno local.
 */
const DB_HOST = "localhost";
const DB_USER = "ds6";
const DB_PASSWORD = "123";
const DB_NAME = "turno_academia";

/**
 * Crea y retorna una conexión nueva a MySQL.
 *
 * @throws RuntimeException cuando no se puede establecer la conexión.
 */
function createConnection(): mysqli
{
    $connection = new mysqli(DB_HOST, DB_USER, DB_PASSWORD, DB_NAME);
    if ($connection->connect_error) {
        throw new RuntimeException("Conexión fallida: " . $connection->connect_error);
    }
    $connection->set_charset("utf8mb4");
    return $connection;
}

/**
 * Permite probar la conexión accediendo directamente a config.php en el navegador.
 */
if (php_sapi_name() !== 'cli' && realpath(__FILE__) === realpath($_SERVER['SCRIPT_FILENAME'] ?? '')) {
    header('Content-Type: application/json; charset=utf-8');
    try {
        $connection = createConnection();
        echo json_encode(["success" => true, "message" => "Conexión a la base de datos exitosa."]);
        $connection->close();
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => $e->getMessage()]);
    }
}
?>
