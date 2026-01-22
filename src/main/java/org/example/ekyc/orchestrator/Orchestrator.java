package org.example.ekyc.orchestrator;

import org.example.ekyc.decision.KYCDecisionEngine;
import org.example.ekyc.exception.HttpStatusServiceException;
import org.example.ekyc.exception.ServiceException;
import org.example.ekyc.exception.ValidationException;
import org.example.ekyc.models.*;
import org.example.ekyc.service.VerificationClient;
import org.example.ekyc.validation.CustomerValidator;
import org.example.sdk.retry.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Orchestrator {
    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);
    public static final String CORRELATION_ID = "correlationID";
    private final Map<VerificationType, VerificationClient> clients;
    private final KYCDecisionEngine decisionEngine;
    private final RetryExecutor retryExecutor;

    public Orchestrator(List<VerificationClient> clients, KYCDecisionEngine decisionEngine, RetryExecutor retryExecutor) {
        this.clients = Objects.requireNonNull(clients, "clients").stream()
                .collect(Collectors.toUnmodifiableMap(VerificationClient::type, Function.identity()));
        this.decisionEngine = decisionEngine;
        this.retryExecutor = retryExecutor;
    }


    public KycDecision verify(VerificationRequest request) {
        if( request == null ) {
            throw new ValidationException("request is required");
        }

        if( request.requestID() == null || request.requestID().isBlank() ) {
            throw new ValidationException("requestID is required");
        }

        if(request.verificationType() == null || request.verificationType().isEmpty()) {
            throw new ValidationException("verificationType is required");
        }

        CustomerValidator.validate(request.customer());
        String correlationID = request.requestID();
        MDC.put(CORRELATION_ID, correlationID);
        try {
            log.info("Validating request ID {} customer_id= {} types = {}", correlationID, request.requestID(), request.verificationType());

            List<VerificationResult> results = new ArrayList<>();

            if (request.verificationType().contains(VerificationType.SANCTIONS)) {
                results.add(callWithRetry(VerificationType.SANCTIONS, request, true));
            }

            for (VerificationType type : request.verificationType()) {
                if (type == VerificationType.SANCTIONS) {
                    continue;
                }

                results.add(callWithRetry(type, request, false));
            }

            KycDecision decision = decisionEngine.decide(results);
            log.info("Successfully decided {} for request_id = {}", decision, request.requestID());
            return decision;
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }

    private VerificationResult callWithRetry(VerificationType verificationType, VerificationRequest request, boolean isCritical) {
        VerificationClient client = clients.get(verificationType);

        if(client == null) {
            throw new ValidationException("client not found");
        }

        try{
            return retryExecutor.run(() -> client.verify(request));
        } catch(ValidationException e) {
            throw e;
        } catch(ServiceException e){
            if(isCritical){
                throw e;
            }

            log.warn("service failed type = {} msg = {}", verificationType, e.getMessage());
            return new VerificationResult(verificationType, VerificationStatus.MANUAL_REVIEW, 0, List.of("SERVICE FAILURE"), Instant.now());
        } catch(RuntimeException e){
            if(isCritical){
                throw e;
            }
            log.warn("unknown service failure type = {} msg = {}", verificationType, e.getMessage());
            return new VerificationResult(verificationType, VerificationStatus.MANUAL_REVIEW, 0, List.of("UNKNOWN"), Instant.now());
        }
    }

    public static RetryExecutor.RetryPredicate defaultRetryPredicate() {
        return t -> {
            if( t instanceof TimeoutException) {
                return true;
            }

            if( t instanceof HttpStatusServiceException hse) {
                int code = hse.getCode();
                return code >= 500 && code < 600;
            }
            return false;
        };
    }
}
