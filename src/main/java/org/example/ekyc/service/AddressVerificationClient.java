package org.example.ekyc.service;

import org.example.ekyc.config.KYCConfig;
import org.example.ekyc.http.HttpClient;
import org.example.ekyc.models.VerificationRequest;
import org.example.ekyc.models.VerificationResult;
import org.example.ekyc.models.VerificationStatus;
import org.example.ekyc.models.VerificationType;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.example.ekyc.service.Endpoints.ADDRESS_VERIFY_PATH;

public class AddressVerificationClient implements VerificationClient{
    public static final int CONFIDENCE_THRESHOLD = 80;
    private final KYCConfig config;
    private final HttpClient httpClient;
    public static final int WINDOW = 90;

    public AddressVerificationClient(KYCConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public VerificationType type() {
        return VerificationType.ADDRESS;
    }

    public record AddressRequest(
            String customer_id,
            String address,
            String proof_type,
            String proof_date,
            String proof_url
    ){}

    public record AddressResponse(String status, int confidence, List<String> reasons) {}

    @Override
    public VerificationResult verify(VerificationRequest verificationRequest) {
        if(verificationRequest.addressInput() == null){
            throw new IllegalArgumentException("address input is null");
        }

        if(verificationRequest.addressInput().proofDate() == null){
            throw new IllegalArgumentException("proof date input is null");
        }

        long days = ChronoUnit.DAYS.between(Instant.now(), verificationRequest.addressInput().proofDate());

        if(days> WINDOW){
            return new VerificationResult(
                    VerificationType.ADDRESS,
                    VerificationStatus.MANUAL_REVIEW,
                    0,
                    List.of("TOO_OLD_PROOF"),
                    Instant.now()
            );
        }

         org.example.ekyc.service.AddressVerificationClient.AddressRequest addressRequest = new AddressRequest(
                 verificationRequest.customer().customerId(),
                 verificationRequest.customer().address(),
                 verificationRequest.addressInput().proofType(),
                 verificationRequest.addressInput().proofDate().toString(),
                 verificationRequest.addressInput().proofUrl()
         );

        String url = config.addressServiceBaseURL() + ADDRESS_VERIFY_PATH;
        AddressResponse response = httpClient.postJSON(
                    url,addressRequest, AddressResponse.class, config.addressServiceTimeout()
        );

        boolean pass = response.status() != null && response.status.equalsIgnoreCase("PASS");

        if(pass && response.confidence > CONFIDENCE_THRESHOLD){
            return new VerificationResult(
                    VerificationType.ADDRESS,
                    VerificationStatus.PASS,
                    response.confidence,
                    List.of(),
                    Instant.now()
            );

        }

        return new VerificationResult(
                VerificationType.ADDRESS,
                VerificationStatus.MANUAL_REVIEW,
                Math.max(0, Math.min(100, response.confidence)),
                (response.reasons == null || response.reasons.isEmpty())
                        ? List.of("LOW_CONFIDENCE_OR_FAIL") : response.reasons,
                Instant.now()
        );
    }
}
