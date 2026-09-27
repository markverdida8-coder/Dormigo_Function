<?php
require_once 'db.php';
try {
    // Add 'COMPLETED' to booking_status_enum if not already present
    $pdo->exec("ALTER TYPE booking_status_enum ADD VALUE IF NOT EXISTS 'COMPLETED';");
    echo json_encode(["success" => true, "message" => "Added COMPLETED to booking_status_enum."]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>
