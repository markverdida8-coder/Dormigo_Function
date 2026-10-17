<?php

function subscriptionRespond(int $statusCode, array $payload): void
{
    http_response_code($statusCode);
    echo json_encode($payload);
    exit();
}

function requireSubscriptionRole(array $user, string $role): int
{
    if (strtoupper((string)($user['user_type'] ?? '')) !== $role) {
        subscriptionRespond(403, ['success' => false, 'message' => 'You are not authorized for this subscription action.']);
    }

    return (int)$user['user_id'];
}

function subscriptionSchemaReady(PDO $pdo): bool
{
    $stmt = $pdo->query("SELECT to_regclass('public.landlord_subscriptions') IS NOT NULL");
    return in_array($stmt->fetchColumn(), [true, 't', '1', 1], true);
}

function activeLandlordSubscription(PDO $pdo, int $landlordId): ?array
{
    $stmt = $pdo->prepare(
        "SELECT subscription_id, plan_name, amount, currency, payment_method, transaction_ref,
                starts_at, expires_at, status
         FROM landlord_subscriptions
         WHERE landlord_id = :landlord_id
           AND status = 'SUCCESSFUL'
           AND starts_at <= CURRENT_DATE
           AND expires_at > CURRENT_DATE
         ORDER BY expires_at DESC, subscription_id DESC
         LIMIT 1"
    );
    $stmt->execute(['landlord_id' => $landlordId]);
    $subscription = $stmt->fetch(PDO::FETCH_ASSOC);

    return $subscription ?: null;
}
