# Meeting Webhook Application

Event-driven webhook ingestion service for a video meeting platform. The service receives webhook events for meeting lifecycle changes and live transcription, processes them asynchronously through an event-driven architecture, and persists the results to a database.

**Build & Run**

- **Prerequisites**: Java 17+ (JDK), Git. The project uses the Gradle wrapper so you do not need a local Gradle install.
- **Build (Windows)**: `gradlew.bat build`
- **Build (Unix/macOS)**: `./gradlew build`
- **Run (development with Gradle)**: `gradlew.bat bootRun` or `./gradlew bootRun`
- **Run the built JAR**: `java -jar build/libs/*.jar`
- **Run tests**: `gradlew.bat test` or `./gradlew test`

Configuration

- Application configuration is in `src/main/resources/application.properties` and can be overridden with environment variables or Spring `--spring.*` properties. The app defaults to port 8080 unless overridden via `server.port`.

**Quick Dev Tips**

- Use the Gradle wrapper (`gradlew` / `gradlew.bat`) to ensure consistent builds across machines.
- To run on Windows PowerShell:

```powershell
# from repo root
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

**Architecture Overview**

The project follows a layered architecture with clear package separation:

- `com.github.meeting_platform.application`: Application services and orchestration (use-cases).
- `com.github.meeting_platform.domain`: Core domain models, business rules, and domain services.
- `com.github.meeting_platform.infrastructure`: Adapters and integrations (persistence, external HTTP clients, etc.).
- `com.github.meeting_platform.config`: Spring configuration and beans.
- `com.github.meeting_platform.common`: Shared utilities and exception types.
- `com.github.meeting_platform.application.events` and `application.eventhandler`: Event definitions and simple in-process event handlers used for decoupling side effects.

This layout keeps domain logic independent of framework and infrastructure concerns, improving testability and clarity.

**Design Decisions & Trade-offs**

- **Layered, DDD-influenced structure**: Explicit separation of concerns (application, domain, infrastructure) makes business rules easy to test and change. Trade-off: more files and indirection for simple features.
- **Spring Boot + Gradle wrapper**: Provides fast developer feedback loops and a familiar ecosystem. Trade-off: increased dependency surface and startup overhead compared to minimal frameworks.
- **In-process event handling**: Events are handled inside the JVM using Spring Application Events to keep the initial implementation simple and easy to test. Trade-off: easier to run locally but less resilient than using an external message broker (Kafka, RabbitMQ) for high scale or cross-process delivery. For production scale, consider migrating to an external message broker.
- **Embedded H2 database**: Keeps the repository lightweight and easy to run locally without external dependencies. Trade-off: data is not persisted across restarts. For production, configure an external database (PostgreSQL, MySQL) via `application.properties`.
- **Spring Retry for transient failures**: Automatic retry with exponential backoff for database connection issues. Trade-off: adds complexity but improves reliability for transient failures. Non-retryable errors (validation, business rules) fail fast.
- **Idempotent operations**: Duplicate webhook deliveries are handled gracefully - duplicate transcripts and sessions are silently ignored. Trade-off: requires careful design but ensures reliability in distributed systems.
- **Async processing**: Transcript and ended events are processed asynchronously to improve webhook response time. Trade-off: eventual consistency - webhook returns 202 Accepted immediately, processing happens asynchronously.

**Error Handling Strategy**

- **Global Exception Handler**: All exceptions are caught by `GlobalExceptionHandler` and converted to appropriate HTTP status codes:
  - `400 Bad Request`: Invalid input, validation errors, malformed payloads
  - `404 Not Found`: Meeting or session not found
  - `409 Conflict`: Duplicate entries (idempotent operations), session already ended
  - `500 Internal Server Error`: Unexpected errors
- **Retry Logic**: Transient failures (database connection issues) are automatically retried up to 3 times with exponential backoff (1s, 2s, 4s). Non-retryable errors (validation, business rules) fail immediately.
- **Event Processing Errors**: Errors in async event processing are logged but don't crash the application. Failed events are logged with full context for debugging.
- **Validation**: Webhook payloads are validated for required fields before processing. Missing or invalid fields return 400 Bad Request with descriptive error messages.

**Idempotency Guarantees**

- **Transcripts**: Duplicate transcripts (same `transcriptId`) are silently ignored. The system checks for existing transcripts before saving.
- **Sessions**: Duplicate `meeting.started` events for the same `sessionId` are idempotent if the session is already LIVE. Attempting to start an ENDED session throws an error.
- **Meetings**: Multiple sessions can exist for the same meeting (concurrent or sequential sessions are supported).

**Retry & Fallback Mechanisms**

- **Spring Retry**: Configured with 3 retry attempts, exponential backoff (1s initial, 2x multiplier, max 5s interval)
- **Retryable Exceptions**: Only `DataAccessException` (database connection issues) triggers retries
- **Non-Retryable**: Validation errors, business rule violations, and session state errors fail immediately
- **Back-pressure**: Thread pool executor uses `CallerRunsPolicy` to handle queue overflow - when queue is full, caller thread executes the task
- **Future Enhancement**: For production, consider implementing a dead letter queue (DLQ) to store failed events after max retries for manual review/replay

**Assumptions & Limitations**

- Developers have JDK 17+ installed and can run the Gradle wrapper
- Local development uses embedded H2 database (data not persisted across restarts)
- Production deployments should configure external database (PostgreSQL, MySQL) via `application.properties`
- The service listens on port 8080 unless overridden via `server.port`
- Webhook payloads use `transcriptId` as the deduplication key for transcripts
- Transcript offsets support both formats: `"HH:MM:SS.millis"` (e.g., `"00:12:34.567"`) and integer seconds for backward compatibility
- Sessions must be LIVE to accept transcripts - transcripts arriving after `meeting.ended` are rejected
- No webhook signature verification implemented (can be added for production)
- No API versioning implemented (can be added if needed)

**Testing**

**Run Unit Tests:**
```bash
./gradlew test
# or on Windows
gradlew.bat test
```

**Run Simulation Scripts:**

1. **Happy Path Simulation:**
```bash
# Linux/macOS
./scripts/simulate_meeting.sh

# Windows PowerShell
powershell -File scripts/simulate_meeting.sh

# With custom base URL
./scripts/simulate_meeting.sh http://localhost:8080
```

2. **Edge Cases Simulation:**
```bash
# Linux/macOS
./scripts/simulate_meeting_edge_cases.sh

# Windows PowerShell
powershell -File scripts/simulate_meeting_edge_cases.sh
```

The edge case script tests:
- Duplicate transcript chunks (idempotency)
- Out-of-order transcript delivery
- Transcripts arriving after meeting ended
- Meeting ended without meeting started
- Concurrent sessions for the same meeting
- Duplicate meeting.started events
- Corrupted payloads (missing fields)
- Large transcript payloads

**Verify Transcript Retrieval:**

After running a simulation, retrieve the transcript:
```bash
curl http://localhost:8080/api/meetings/{meetingId}/sessions/{sessionId}/transcript
```

**API Endpoints**

- `POST /api/webhooks` - Webhook ingestion endpoint
  - Accepts: `meeting.started`, `meeting.transcript`, `meeting.ended` events
  - Returns: `202 Accepted` immediately, processes asynchronously
- `GET /api/meetings/{meetingId}/sessions/{sessionId}/transcript` - Retrieve ordered transcript for a session
  - Returns: List of transcript segments ordered by `sequenceNumber`
- `GET /api/webhooks/health` - Health check endpoint
  - Returns: `200 OK` with status

**Docker Support**

Build and run with Docker:
```bash
docker-compose up --build
```

Or build Docker image:
```bash
docker build -t meeting-platform .
docker run -p 8080:8080 meeting-platform
```
