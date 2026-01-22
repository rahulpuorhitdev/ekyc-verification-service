eKYC Verification Service (Java)

This repository implements the take-home assessment milestones:
- Milestone 1: core orchestration + decision engine + unit tests
- Milestone 2: structured logging w/ correlation IDs, retries w/ exponential backoff, custom exceptions
- Milestone 3 (Option A): per-service rate limiting (10 req/min) with unit test

## Requirements
- Java 17+
- Maven 3.9+

## Run tests

```bash
mvn test
```

## Design notes
- **No real HTTP calls in tests**: service calls are mocked via `HttpClient` / `VerificationClient`.
- **Correlation IDs**: `EkycOrchestrator` uses SLF4J MDC key `correlation_id` sourced from `requestId`.
- **PII safety**: logs include only `request_id`, `customer_id`, and verification types (no name/DOB/address).
- **Retry policy**: retries on timeouts and HTTP 5xx only (no retry on 4xx), with backoff 1s/2s/4s.
- **Sanctions is critical**: sanctions failures throw; other service failures downgrade to `MANUAL_REVIEW`.