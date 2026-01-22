package org.example.ekyc.models;

import java.time.Instant;
import java.util.List;

public record KycDecision(
        Decision decision,
        List<VerificationResult> verificationResults,
        Instant timestamp
) {
    public KycDecision {
        if( decision == null ){
            throw new IllegalArgumentException("Decision is required");
        }

        if( verificationResults == null ){
            throw new IllegalArgumentException("Verification results is required");
        }

        if( timestamp == null ){
            throw new IllegalArgumentException("Timestamp is required");
        }
    }

    public enum Decision {
        APPROVE,
        REJECT,
        MANUAL_REVIEW
    }
}
