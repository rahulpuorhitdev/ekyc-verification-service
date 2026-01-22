package org.example.ekyc.service;

import org.example.ekyc.models.VerificationRequest;
import org.example.ekyc.models.VerificationResult;
import org.example.ekyc.models.VerificationType;

public interface VerificationClient {
    VerificationType type();

    VerificationResult verify(VerificationRequest verificationRequest);
}
