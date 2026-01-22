package org.example.ekyc.config;

import java.time.Duration;

public record KYCConfig(
        String addressServiceBaseURL,
        String documentServiceBaseURL,
        String biometricServiceBaseURL,
        String sanctionServiceBaseURL,
        Duration addressServiceTimeout,
        Duration documentServiceTimeout,
        Duration biometricServiceTimeout,
        Duration sanctionServiceTimeout
) {
}
