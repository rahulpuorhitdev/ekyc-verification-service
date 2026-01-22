package org.example.ekyc.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record VerificationResult (
        VerificationType verificationType,
        VerificationStatus status,
        int confidence,
        List<String> reasons,
        Instant timestamp
){
    public VerificationResult {
        if(verificationType == null){
            throw new IllegalArgumentException("VerificationType is required");
        }

        if(status == null){
            throw new IllegalArgumentException("VerificationStatus is required");
        }

        if(reasons == null){
            reasons = new ArrayList<>();
        }

        if(confidence < 0){
            throw new IllegalArgumentException("Confidence is required");
        }
    }
}
