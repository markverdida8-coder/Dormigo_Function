<?php
if (isset($_SERVER['HTTP_ORIGIN'])) {
    header("Access-Control-Allow-Origin: {$_SERVER['HTTP_ORIGIN']}");
    header("Access-Control-Allow-Credentials: true");
} else {
    header("Access-Control-Allow-Origin: *");
}
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS");
header("Content-Type: application/json; charset=UTF-8");

if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$getEnvironmentValue = static function (string $name): ?string {
    $value = getenv($name);
    return ($value === false || $value === '') ? null : $value;
};

$requiredEnvironment = [
    'DORMIGO_DB_HOST',
    'DORMIGO_DB_PORT',
    'DORMIGO_DB_NAME',
    'DORMIGO_DB_USER',
    'DORMIGO_DB_PASSWORD',
];
$databaseConfig = [];
$missingEnvironment = [];

foreach ($requiredEnvironment as $name) {
    $value = $getEnvironmentValue($name);
    if ($value === null) {
        $missingEnvironment[] = $name;
    } else {
        $databaseConfig[$name] = $value;
    }
}

if ($missingEnvironment) {
    http_response_code(500);
    error_log('Dormigo database configuration is incomplete: ' . implode(', ', $missingEnvironment));
    echo json_encode([
        "success" => false,
        "message" => "Database configuration is incomplete. Contact the administrator."
    ]);
    exit();
}

$host     = $databaseConfig['DORMIGO_DB_HOST'];
$port     = $databaseConfig['DORMIGO_DB_PORT'];
$dbname   = $databaseConfig['DORMIGO_DB_NAME'];
$user     = $databaseConfig['DORMIGO_DB_USER'];
$password = $databaseConfig['DORMIGO_DB_PASSWORD'];

try {
    $dsn = "pgsql:host=$host;port=$port;dbname=$dbname;";
    $pdo = new PDO($dsn, $user, $password, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);
    $pdo->exec("SET search_path TO public;");
} catch (PDOException $e) {
    error_log('Dormigo database connection failed: ' . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => "Database connection failed. Contact the administrator."
    ]);
    exit();
}
?>
