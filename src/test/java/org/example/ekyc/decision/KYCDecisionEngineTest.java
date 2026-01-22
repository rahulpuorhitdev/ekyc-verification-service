package org.example.ekyc.decision;


import org.example.ekyc.models.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KYCDecisionEngineTest {

    private final KYCDecisionEngine kycDecisionEngine = new KYCDecisionEngine();

    @Test
    void all_verification_pass_approved(){
        List<VerificationResult>  verificationResults = List.of(
                new VerificationResult(VerificationType.SANCTIONS, VerificationStatus.PASS, 0, null, null),
                new VerificationResult(VerificationType.ID_DOCUMENT, VerificationStatus.PASS, 95, null, null),
                new VerificationResult(VerificationType.FACE_MATCH, VerificationStatus.PASS, 97, null, null ),
                new VerificationResult(VerificationType.ADDRESS, VerificationStatus.PASS, 95, null, null )
                );

        KycDecision decision = kycDecisionEngine.decide(verificationResults);
        assertEquals(KycDecision.Decision.APPROVE, decision.decision());
    }

    @Test
    void sanctions_hit_rejected(){
        List<VerificationResult>  verificationResults = List.of(
                new VerificationResult(VerificationType.SANCTIONS, VerificationStatus.FAIL, 0, List.of("Test_Reason"), Instant.now()),
                new VerificationResult(VerificationType.ID_DOCUMENT, VerificationStatus.PASS, 95, null, null),
                new VerificationResult(VerificationType.FACE_MATCH, VerificationStatus.PASS, 96, null, null ),
                new VerificationResult(VerificationType.ADDRESS, VerificationStatus.PASS, 96, null, null )
                );

        KycDecision decision = kycDecisionEngine.decide(verificationResults);
        assertEquals(KycDecision.Decision.REJECT, decision.decision());
    }

    @Test
    void low_confidence_face_match_manual_review(){
        List<VerificationResult>  verificationResults = List.of(
                new VerificationResult(VerificationType.SANCTIONS, VerificationStatus.PASS, 0, List.of("Test_Reason"), Instant.now()),
                new VerificationResult(VerificationType.ID_DOCUMENT, VerificationStatus.PASS, 95, null, null),
                new VerificationResult(VerificationType.FACE_MATCH, VerificationStatus.MANUAL_REVIEW, 70, List.of("Manual Review reason"), Instant.now() ),
                new VerificationResult(VerificationType.ADDRESS, VerificationStatus.PASS, 88, null, null )
        );

        KycDecision decision = kycDecisionEngine.decide(verificationResults);
        assertEquals(KycDecision.Decision.MANUAL_REVIEW, decision.decision());
    }

    @Test
    void expired_document_rejected(){
        List<VerificationResult>  verificationResults = List.of(
                new VerificationResult(VerificationType.SANCTIONS, VerificationStatus.PASS
                        , 0, List.of("Test_Reason"), Instant.now()),
                new VerificationResult(VerificationType.ID_DOCUMENT, VerificationStatus.FAIL, 95, List.of("Expired Document"), Instant.now() ),
                new VerificationResult(VerificationType.FACE_MATCH, VerificationStatus.PASS, 70, List.of("Manual Review reason"), Instant.now() ),
                new VerificationResult(VerificationType.ADDRESS, VerificationStatus.PASS, 88, null, null )
        );
    }
}
