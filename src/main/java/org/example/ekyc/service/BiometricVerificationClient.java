package org.example.ekyc.service;

import org.example.ekyc.config.KYCConfig;
import org.example.ekyc.http.HttpClient;
import org.example.ekyc.models.VerificationRequest;
import org.example.ekyc.models.VerificationResult;
import org.example.ekyc.models.VerificationStatus;
import org.example.ekyc.models.VerificationType;

import java.time.Instant;
import java.util.List;

public class BiometricVerificationClient implements VerificationClient {

    public static final int CONFIDENCE_THRESHOLD = 85;
    public static final double SIMILARITY_THRESHOLD = 85.0;
    private HttpClient httpClient;
    private KYCConfig kycConfig;

    public BiometricVerificationClient(HttpClient httpClient, KYCConfig kycConfig) {
        this.httpClient = httpClient;
        this.kycConfig = kycConfig;
    }

    record FaceMatchRequest(String customer_id, String selfie_url, String id_photo_url){

    }

    record FaceMatchResponse(String status, int confidence, int similarity_score){

    }

    @Override
    public VerificationType type() {
        return VerificationType.FACE_MATCH;
    }

    @Override
    public VerificationResult verify(VerificationRequest verificationRequest) {
        if(verificationRequest.faceMatchInput() == null){
            throw new NullPointerException("FaceMatchInput is null");
        }

        if(verificationRequest.faceMatchInput().selfieURL() == null){
            throw new NullPointerException("URL is null");
        }

        if(verificationRequest.faceMatchInput().idPhotoURL() == null){
            throw new NullPointerException("PhotoURL is null");
        }

        FaceMatchRequest request = new FaceMatchRequest(
                verificationRequest.customer().customerId(),
                verificationRequest.faceMatchInput().selfieURL(),
                verificationRequest.faceMatchInput().idPhotoURL()
        );

        String url = kycConfig.biometricServiceBaseURL() + Endpoints.FACE_MATCH_PATH;
        FaceMatchResponse response = httpClient.postJSON(url, request, FaceMatchResponse.class, kycConfig.biometricServiceTimeout());

        boolean pass = response.status() != null && response.status.equalsIgnoreCase("PASS");
        boolean verificationPass = pass && response.confidence() > CONFIDENCE_THRESHOLD && response.similarity_score > SIMILARITY_THRESHOLD;

        if(verificationPass){
            return new VerificationResult(
                    VerificationType.FACE_MATCH,
                    VerificationStatus.PASS,
                    response.confidence,
                    List.of(),
                    Instant.now()
            );
        }

        return new VerificationResult(
                VerificationType.FACE_MATCH,
                VerificationStatus.MANUAL_REVIEW,
                Math.max(0, Math.min(100, response.confidence)),
                List.of("LOW_CONFIDENCE_OR_SIMILARITY"),
                Instant.now()
        );
    }

    private static int getAnInt() {
        return 85;
    }
}
