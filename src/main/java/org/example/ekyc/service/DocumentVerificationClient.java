package org.example.ekyc.service;

import org.example.ekyc.config.KYCConfig;
import org.example.ekyc.http.HttpClient;
import org.example.ekyc.models.VerificationRequest;
import org.example.ekyc.models.VerificationResult;
import org.example.ekyc.models.VerificationStatus;
import org.example.ekyc.models.VerificationType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class DocumentVerificationClient implements VerificationClient {

    public static final int THRESHOLD = 85;
    private final HttpClient httpClient;
    private final KYCConfig config;

    public DocumentVerificationClient(HttpClient httpClient, KYCConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    record DocumentRequest(
            String customer_id,
            String document_type,
            String document_number,
            String expiry_date,
            String document_image_url
            ){}

    record DocumentResponse(
       String status,
       int confidence,
       List<String> reasons
    ){}

    @Override
    public VerificationType type() {
        return VerificationType.ID_DOCUMENT;
    }

    @Override
    public VerificationResult verify(VerificationRequest verificationRequest) {
        if(verificationRequest.documentInput() == null){
            throw new IllegalStateException("Document input is null");
        }

        if(verificationRequest.documentInput().expiryDate() == null){
            throw new IllegalStateException("Document expiry date is null");
        }

        LocalDate expiryDate = verificationRequest.documentInput().expiryDate();

        if(expiryDate.isBefore(LocalDate.now())) {
            return new VerificationResult(
                    VerificationType.ID_DOCUMENT,
                    VerificationStatus.FAIL,
                    0,
                    List.of("DOCUMENT_EXPIRED"),
                    Instant.now()
            );
        }

        DocumentRequest body = new DocumentRequest(
                verificationRequest.customer().customerId(),
                verificationRequest.documentInput().documentType(),
                verificationRequest.documentInput().documentNumber(),
                verificationRequest.documentInput().expiryDate().toString(),
                verificationRequest.documentInput().documentImageUrl()
        );

        String url = config.documentServiceBaseURL() + Endpoints.DOCUMENT_VERIFY_PATH;

        DocumentResponse response = httpClient.postJSON(
                url, body, DocumentResponse.class, config.documentServiceTimeout()
        );

        int confidence = response.confidence();
        boolean pass = response.status() != null && response.status.equalsIgnoreCase("PASS");

        if(pass && confidence > THRESHOLD){
            return new VerificationResult(
                    VerificationType.ID_DOCUMENT,
                    VerificationStatus.PASS,
                    confidence,
                    List.of(),
                    Instant.now()
            );
        }

        return new VerificationResult(
                VerificationType.ID_DOCUMENT,
                VerificationStatus.MANUAL_REVIEW,
                confidence,
                (response.reasons() == null || response.reasons().isEmpty()) ? List.of("LOW_CONFIDENCE_OR_FAIL") : response.reasons(),
                Instant.now()
        );
    }
}
