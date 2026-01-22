package org.example.ekyc.decision;

import org.example.ekyc.models.KycDecision;
import org.example.ekyc.models.VerificationResult;
import org.example.ekyc.models.VerificationStatus;
import org.example.ekyc.models.VerificationType;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class KYCDecisionEngine {
    public KycDecision decide(List<VerificationResult> verificationResults){
        boolean sanctionHit = verificationResults.stream().anyMatch(
                r -> r.verificationType() == VerificationType.SANCTIONS
        );

        if(!sanctionHit){
            return new KycDecision(KycDecision.Decision.MANUAL_REVIEW, verificationResults, Instant.now());
        }

        VerificationResult sanctions = verificationResults.stream()
                .filter(r -> r.verificationType() == VerificationType.SANCTIONS)
                .max(Comparator.comparing(VerificationResult::timestamp))
                .orElseThrow();

        if(sanctions.status() == VerificationStatus.FAIL){
            return new KycDecision(KycDecision.Decision.REJECT, verificationResults, Instant.now());
        }

        boolean expiredDocument = verificationResults.stream()
                .filter(r->r.verificationType() == VerificationType.ID_DOCUMENT)
                .anyMatch(
                        r -> r.reasons() != null
                                && r.reasons().stream().anyMatch("Expired Document"::equalsIgnoreCase
                        )
                );

        if(expiredDocument){
            new KycDecision(KycDecision.Decision.REJECT, verificationResults, Instant.now());
        }

        boolean anyFail = verificationResults.stream().anyMatch(
                r -> r.status() == VerificationStatus.FAIL);

        if(anyFail){
            return new KycDecision(KycDecision.Decision.REJECT, verificationResults, Instant.now());
        }

        boolean manualReview = verificationResults.stream().anyMatch(r -> r.status() == VerificationStatus.MANUAL_REVIEW);

        if(manualReview){
            return new KycDecision(KycDecision.Decision.MANUAL_REVIEW, verificationResults, Instant.now());
        }

        boolean allPass = verificationResults.stream().allMatch(r -> r.status() == VerificationStatus.PASS);

        if(allPass){
            return new KycDecision(KycDecision.Decision.APPROVE, verificationResults, Instant.now());
        }

        return new KycDecision(KycDecision.Decision.MANUAL_REVIEW, verificationResults, Instant.now());
    }
}
