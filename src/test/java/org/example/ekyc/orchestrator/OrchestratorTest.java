package org.example.ekyc.orchestrator;

import org.example.ekyc.decision.KYCDecisionEngine;
import org.example.ekyc.exception.ServiceException;
import org.example.ekyc.models.*;
import org.example.ekyc.service.VerificationClient;
import org.example.sdk.retry.RetryExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrchestratorTest {
    @Test
    void happy_path_approved(){
        VerificationClient doc = mock(VerificationClient.class);
        when(doc.type()).thenReturn(VerificationType.ID_DOCUMENT);
        when(doc.verify(any())).thenReturn(result(VerificationType.ID_DOCUMENT, VerificationStatus.PASS));

        VerificationClient face = mock(VerificationClient.class);
        when(face.type()).thenReturn(VerificationType.FACE_MATCH);
        when(face.verify(any())).thenReturn(result(VerificationType.FACE_MATCH, VerificationStatus.PASS));

        VerificationClient address = mock(VerificationClient.class);
        when(address.type()).thenReturn(VerificationType.ADDRESS);
        when(address.verify(any())).thenReturn(result(VerificationType.ADDRESS, VerificationStatus.PASS));

        VerificationClient sanctions = mock(VerificationClient.class);
        when(sanctions.type()).thenReturn(VerificationType.SANCTIONS);
        when(sanctions.verify(any())).thenReturn(result(VerificationType.SANCTIONS, VerificationStatus.PASS));

        Orchestrator orchestrator = new Orchestrator(
                List.of(doc, face, address, sanctions),
                new KYCDecisionEngine(),
                new RetryExecutor(3, ms -> {}, Orchestrator.defaultRetryPredicate())
        );

        VerificationRequest request = request(
                List.of(
                    VerificationType.SANCTIONS,
                        VerificationType.ID_DOCUMENT,
                        VerificationType.FACE_MATCH,
                        VerificationType.ADDRESS
                ));

        KycDecision decision = orchestrator.verify(request);
        assertEquals(KycDecision.Decision.APPROVE, decision.decision());
    }

    @Test
    void sanction_failure_exception(){
        VerificationClient sanctions = mock(VerificationClient.class);
        when(sanctions.type()).thenReturn(VerificationType.SANCTIONS);
        when(sanctions.verify(any())).thenThrow(new ServiceException("sanctions", "boom"));

        VerificationClient doc = mock(VerificationClient.class);
        when(doc.type()).thenReturn(VerificationType.ID_DOCUMENT);
        when(doc.verify(any())).thenReturn(result(VerificationType.ID_DOCUMENT, VerificationStatus.PASS));

        Orchestrator orchestrator = new Orchestrator(
                List.of(doc, sanctions),
                new KYCDecisionEngine(),
                new RetryExecutor(3, ms ->{}, Orchestrator.defaultRetryPredicate())
        );

        assertThrows(ServiceException.class, () -> orchestrator.verify(request(List.of(VerificationType.SANCTIONS))));
    }

    @Test
    void non_critical_failure_exception_manual_review(){
        VerificationClient sanctions = mock(VerificationClient.class);
        when(sanctions.type()).thenReturn(VerificationType.SANCTIONS);
        when(sanctions.verify(any())).thenReturn(result(VerificationType.SANCTIONS, VerificationStatus.PASS));

        VerificationClient face = mock(VerificationClient.class);
        when(face.type()).thenReturn(VerificationType.FACE_MATCH);
        when(face.verify(any())).thenThrow(new ServiceException("biometric", "boom"));

        Orchestrator orchestrator = new Orchestrator(
                List.of(sanctions, face),
                new KYCDecisionEngine(),
                new RetryExecutor(3, ms -> {}, Orchestrator.defaultRetryPredicate())
        );

        KycDecision decision = orchestrator.verify(request(List.of(VerificationType.SANCTIONS, VerificationType.FACE_MATCH)));
        assertEquals(KycDecision.Decision.MANUAL_REVIEW, decision.decision());
    }

    private static VerificationResult result(VerificationType type, VerificationStatus status) {
        return new VerificationResult(
                type,
                status,
                95,
                List.of(),
                Instant.now()
        );
    }

    private static VerificationRequest request(List<VerificationType> types) {
        Customer customer = new Customer(
                "TEST_CUST",
                "Test Name",
                LocalDate.parse("1991-01-31"),
                "TEST_CUST@example.com",
                "+1-323523542",
                "12q34235 sdsafd af safa",
                "US"
        );

        return new VerificationRequest(
                "REQ-TEST",
                customer,
                types,
                Instant.parse("2026-01-12T10:30:00Z"),
                new VerificationRequest.DocumentInput("PASSPORT", "TESTPASS", LocalDate.parse("2027-12-31"), "http://example.doc"),
                new VerificationRequest.FaceMatchInput("example.json", "example.jaosn"),
                new VerificationRequest.AddressInput("BILL", LocalDate.parse("2025-12-30"), "json")
        );
    }
}
