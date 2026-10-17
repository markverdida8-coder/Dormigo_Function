<?php
/**
 * DORMIGO - Secure Mail Helper
 * Pure PHP SMTP mailer with TLS/SSL support and fallback logging.
 */

class MailHelper {

    /**
     * Send OTP email to administrator.
     *
     * @param string $toEmail Recipient email
     * @param string $toName Recipient name
     * @param string $otpCode 6-digit OTP code
     * @param string $purpose 'LOGIN_2FA' or 'PASSWORD_CHANGE'
     * @return array ['success' => bool, 'message' => string, 'debug_code' => string|null]
     */
    public static function sendOtp($toEmail, $toName, $otpCode, $purpose = 'LOGIN_2FA') {
        $config = require __DIR__ . '/mail_config.php';

        $isPasswordChange = ($purpose === 'PASSWORD_CHANGE');
        $subject = $isPasswordChange 
            ? "Dormigo Admin Security Alert: Password Change Authorization Code" 
            : "Dormigo Admin Portal: Your Two-Factor Verification Code";

        $htmlBody = self::buildEmailTemplate($toName, $otpCode, $isPasswordChange);

        // If SMTP is disabled, return success with debug info (local dev mode fallback)
        if (empty($config['smtp_enabled'])) {
            self::logOtpDebug($config, $toEmail, $otpCode, $purpose);
            return [
                'success' => true,
                'message' => 'OTP generated (local development mode).'
            ];
        }

        // Attempt actual SMTP delivery
        $result = self::sendViaSmtp($config, $toEmail, $toName, $subject, $htmlBody);
        if ($result['success']) {
            return [
                'success' => true,
                'message' => 'Verification code sent to your email.'
            ];
        } else {
            error_log("Dormigo SMTP Error: " . $result['error']);
            return [
                'success' => false,
                'message' => 'Failed to send verification email. Please check SMTP configuration or try again later.'
            ];
        }
    }

    /**
     * Build branded responsive HTML email template
     */
    private static function buildEmailTemplate($name, $code, $isPasswordChange) {
        $title = $isPasswordChange ? "Authorize Password Change" : "Admin Two-Factor Sign In";
        $desc = $isPasswordChange 
            ? "A request was made to change the password for your Dormigo Administrator account. Enter this authorization code to confirm the change."
            : "Use the 6-digit code below to complete your sign in to the Dormigo Administration Portal.";

        return '
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; background-color: #F4F7F6; margin: 0; padding: 24px; color: #1E293B; }
            .container { max-width: 540px; margin: 0 auto; background: #FFFFFF; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
            .header { background: #1B5E4C; padding: 28px 32px; text-align: center; color: #FFFFFF; }
            .header h1 { margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.05em; }
            .header p { margin: 4px 0 0; font-size: 12px; color: #D1FAE5; text-transform: uppercase; letter-spacing: 0.1em; }
            .content { padding: 32px; }
            .greeting { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
            .text { font-size: 14px; line-height: 1.6; color: #475569; margin-bottom: 24px; }
            .code-box { background: #F0FDF4; border: 2px dashed #1B5E4C; border-radius: 10px; padding: 20px; text-align: center; margin-bottom: 24px; }
            .code-label { font-size: 12px; font-weight: 700; color: #1B5E4C; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 6px; }
            .code-value { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #0F2C23; font-family: "Courier New", Courier, monospace; }
            .expiry { font-size: 12px; color: #64748B; margin-top: 8px; }
            .warning { background: #FEF3C7; border-left: 4px solid #F59E0B; padding: 12px 16px; font-size: 12px; color: #92400E; border-radius: 4px; margin-bottom: 24px; }
            .footer { background: #F8FAFC; padding: 20px 32px; text-align: center; font-size: 12px; color: #94A3B8; border-top: 1px solid #E2E8F0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>DORMIGO</h1>
              <p>Administration Portal Security</p>
            </div>
            <div class="content">
              <div class="greeting">Hello ' . htmlspecialchars($name) . ',</div>
              <div class="text">' . $desc . '</div>
              <div class="code-box">
                <div class="code-label">' . $title . '</div>
                <div class="code-value">' . htmlspecialchars($code) . '</div>
                <div class="expiry">Expires in <strong>5 minutes</strong> • Single use only</div>
              </div>
              <div class="warning">
                <strong>Security Notice:</strong> Never share this code with anyone. Dormigo administrators or staff will never ask for your verification code. If you did not initiate this request, your account credentials may be compromised.
              </div>
            </div>
            <div class="footer">
              &copy; ' . date('Y') . ' Dormigo Boarding House Finder & Management System. All rights reserved.
            </div>
          </div>
        </body>
        </html>';
    }

    /**
     * Native PHP Socket SMTP client
     */
    private static function sendViaSmtp($config, $toEmail, $toName, $subject, $htmlContent) {
        $timeout = 10;
        $host = $config['host'];
        $port = $config['port'];
        $secure = strtolower($config['secure']);

        $socketHost = ($secure === 'ssl') ? "ssl://{$host}" : $host;
        $socket = @fsockopen($socketHost, $port, $errno, $errstr, $timeout);

        if (!$socket) {
            return ['success' => false, 'error' => "Could not connect to SMTP server {$host}:{$port} ($errstr)"];
        }

        stream_set_timeout($socket, $timeout);

        $getReply = function() use ($socket) {
            $reply = '';
            while ($line = fgets($socket, 515)) {
                $reply .= $line;
                if (substr($line, 3, 1) === ' ') break;
            }
            return $reply;
        };

        $sendCommand = function($cmd) use ($socket, $getReply) {
            fputs($socket, $cmd . "\r\n");
            return $getReply();
        };

        $greeting = $getReply();
        if (substr($greeting, 0, 3) !== '220') {
            fclose($socket);
            return ['success' => false, 'error' => "SMTP greeting failed: {$greeting}"];
        }

        $ehlo1 = $sendCommand("EHLO " . gethostname());
        if (substr($ehlo1, 0, 3) !== '250') {
            fclose($socket);
            return ['success' => false, 'error' => "EHLO failed: {$ehlo1}"];
        }

        // STARTTLS negotiation if TLS configured
        if ($secure === 'tls') {
            $starttls = $sendCommand("STARTTLS");
            if (substr($starttls, 0, 3) === '220') {
                if (!stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLSv1_2_CLIENT | STREAM_CRYPTO_METHOD_TLSv1_3_CLIENT)) {
                    fclose($socket);
                    return ['success' => false, 'error' => "Failed to establish TLS encryption."];
                }
                $ehlo2 = $sendCommand("EHLO " . gethostname());
                if (substr($ehlo2, 0, 3) !== '250') {
                    fclose($socket);
                    return ['success' => false, 'error' => "EHLO post-TLS failed: {$ehlo2}"];
                }
            } else {
                fclose($socket);
                return ['success' => false, 'error' => "STARTTLS failed or rejected: {$starttls}"];
            }
        }

        // AUTH LOGIN
        $auth = $sendCommand("AUTH LOGIN");
        if (substr($auth, 0, 3) !== '334') {
            fclose($socket);
            return ['success' => false, 'error' => "AUTH LOGIN rejected: {$auth}"];
        }

        $userRes = $sendCommand(base64_encode($config['username']));
        if (substr($userRes, 0, 3) !== '334') {
            fclose($socket);
            return ['success' => false, 'error' => "SMTP Username prompt rejected (Code: " . substr($userRes, 0, 3) . ")."];
        }

        $passRes = $sendCommand(base64_encode($config['password']));
        if (substr($passRes, 0, 3) !== '235') {
            $code = substr($passRes, 0, 3);
            fclose($socket);
            return ['success' => false, 'error' => "SMTP Authentication failed with status code {$code}."];
        }

        // MAIL FROM & RCPT TO
        $from = $config['from_email'];
        $mailFrom = $sendCommand("MAIL FROM:<{$from}>");
        if (substr($mailFrom, 0, 3) !== '250') {
            fclose($socket);
            return ['success' => false, 'error' => "MAIL FROM failed: {$mailFrom}"];
        }

        $rcptTo = $sendCommand("RCPT TO:<{$toEmail}>");
        if (substr($rcptTo, 0, 3) !== '250') {
            fclose($socket);
            return ['success' => false, 'error' => "RCPT TO failed: {$rcptTo}"];
        }

        // DATA
        $data = $sendCommand("DATA");
        if (substr($data, 0, 3) !== '354') {
            fclose($socket);
            return ['success' => false, 'error' => "DATA rejected: {$data}"];
        }

        $headers = [
            "MIME-Version: 1.0",
            "Content-Type: text/html; charset=UTF-8",
            "From: {$config['from_name']} <{$from}>",
            "To: {$toName} <{$toEmail}>",
            "Subject: {$subject}",
            "Date: " . date('r'),
            "X-Mailer: Dormigo Security Engine"
        ];

        $message = implode("\r\n", $headers) . "\r\n\r\n" . $htmlContent . "\r\n.\r\n";
        fputs($socket, $message);
        $finalReply = $getReply();

        $sendCommand("QUIT");
        fclose($socket);

        if (substr($finalReply, 0, 3) === '250') {
            return ['success' => true];
        } else {
            return ['success' => false, 'error' => "Message dispatch rejected: {$finalReply}"];
        }
    }

    /**
     * Log OTP to debug log for local developer verification
     */
    private static function logOtpDebug($config, $email, $code, $purpose) {
        $logFile = $config['debug_log'] ?? (__DIR__ . '/../logs/otp_debug.log');
        $dir = dirname($logFile);
        if (!is_dir($dir)) @mkdir($dir, 0777, true);

        $timestamp = date('Y-m-d H:i:s');
        $line = "[{$timestamp}] [{$purpose}] To: {$email} | OTP Code: {$code} (Expires in 5 mins)\n";
        @file_put_contents($logFile, $line, FILE_APPEND);
    }
}
