package org.example.ekyc.service;

import org.example.ekyc.config.KYCConfig;
import org.example.ekyc.http.HttpClient;
import org.example.ekyc.models.VerificationRequest;
import org.example.ekyc.models.VerificationResult;
import org.example.ekyc.models.VerificationStatus;
import org.example.ekyc.models.VerificationType;

import java.time.Instant;
import java.util.List;

public class SanctionScreeningClient implements VerificationClient {

    HttpClient httpClient;
    KYCConfig config;

    public SanctionScreeningClient(HttpClient httpClient, KYCConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    record SanctionRequest(
            String customer_id,
            String full_name,
            String date_of_birth,
            String nationality
    ){}

    record SanctionResponse(
            String status,
            int match_count,
            List<Object> matches
    ) {}


    @Override
    public VerificationType type() {
        return VerificationType.SANCTIONS;
    }

    @Override
    public VerificationResult verify(VerificationRequest verificationRequest) {
        if(verificationRequest.customer() == null){
            throw new IllegalArgumentException("Customer is required");
        }

        if(verificationRequest.customer().dateOfBirth() == null){
            throw new IllegalArgumentException("Date of birth is required");
        }

        if(verificationRequest.customer().nationality() == null){
            throw new IllegalArgumentException("Nationality is required");
        }

        SanctionRequest body = new SanctionRequest(
                verificationRequest.customer().customerId(),
                verificationRequest.customer().fullName(),
                verificationRequest.customer().dateOfBirth().toString(),
                verificationRequest.customer().nationality()
        );

        String url = config.sanctionServiceBaseURL() + Endpoints.SANCTIONS_CHECK_PATH;
        SanctionResponse response = httpClient.postJSON(url, body, SanctionResponse.class, config.sanctionServiceTimeout());

        boolean hit = (response.status() != null && response.status.equalsIgnoreCase("HIT"))
                || response.match_count() > 0;

        if(hit){
            return new VerificationResult(
                    VerificationType.SANCTIONS,
                    VerificationStatus.FAIL,
                    0,
                    List.of("SANCTION_HIT"),
                    Instant.now()
            );

        }

        return new VerificationResult(
                VerificationType.SANCTIONS,
                VerificationStatus.PASS,
                0,
                List.of(),
                Instant.now()
        );
    }
}
