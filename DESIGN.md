# Meeting Platform Webhook Service - Design Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [System Components](#system-components)
3. [Data Flow](#data-flow)
4. [Event-Driven Architecture](#event-driven-architecture)
5. [Error Handling & Resilience](#error-handling--resilience)
6. [Idempotency Strategy](#idempotency-strategy)
7. [Database Schema](#database-schema)
8. [API Design](#api-design)
9. [Scalability Considerations](#scalability-considerations)
10. [Trade-offs & Decisions](#trade-offs--decisions)

## Architecture Overview

The Meeting Platform Webhook Service is an event-driven microservice built with Spring Boot that processes webhook events from a video meeting platform. The service follows Domain-Driven Design (DDD) principles with clear separation between domain logic, application services, and infrastructure concerns.

### High-Level Architecture

```
┌─────────────┐
│   Webhook   │
│   Provider  │
└──────┬──────┘
       │ HTTP POST
       ▼
┌─────────────────────────────────┐
│   WebhookController             │
│   (REST Endpoint)               │
└──────┬──────────────────────────┘
       │ Publishes Events
       ▼
┌─────────────────────────────────┐
│   Spring Application Events     │
│   (In-Process Event Bus)        │
└──────┬──────────────────────────┘
       │ Async Processing
       ▼
┌─────────────────────────────────┐
│   MeetingEventListener          │
│   (Event → Domain Event)        │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│   MeetingEventHandler            │
│   (Business Logic)              │
└──────┬──────────────────────────┘
       │ Calls Service Layer
       ▼
┌─────────────────────────────────┐
│   MeetingService                │
│   (Domain Service)               │
└──────┬──────────────────────────┘
       │ Persists Data
       ▼
┌─────────────────────────────────┐
│   JPA Repositories              │
│   (Data Access)                 │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│   H2 Database (Embedded)        │
└─────────────────────────────────┘
```

## System Components

### 1. Webhook Controller (`WebhookController`)
- **Responsibility**: Receives HTTP webhook requests, validates payloads, and publishes events
- **Key Features**:
  - Validates event type and required fields
  - Returns `202 Accepted` immediately (async processing)
  - Converts JSON payloads to DTOs
  - Publishes events to Spring Application Event Bus

### 2. Event Listener (`MeetingEventListener`)
- **Responsibility**: Listens to webhook DTOs and converts them to domain events
- **Key Features**:
  - Async processing for transcript and ended events
  - Synchronous processing for started events (lower volume)
  - Error handling and logging
  - Converts DTOs to domain events

### 3. Event Handler (`MeetingEventHandler`)
- **Responsibility**: Processes domain events and calls service layer
- **Key Features**:
  - Retry logic for transient failures
  - Error categorization (retryable vs non-retryable)
  - Comprehensive logging

### 4. Domain Service (`MeetingService`)
- **Responsibility**: Implements business logic and orchestrates data operations
- **Key Features**:
  - Idempotency checks
  - Session status validation
  - Transaction management
  - Business rule enforcement

### 5. Repositories
- **MeetingRepository**: CRUD operations for Meeting entities
- **SessionRepository**: CRUD operations for Session entities
- **TranscriptRepository**: CRUD operations for Transcript entities with ordering support

## Data Flow

### Meeting Started Flow

```
1. Webhook POST /api/webhooks
   ↓
2. WebhookController validates payload
   ↓
3. Publishes MeetingStartedWebhookRequest event
   ↓
4. MeetingEventListener (synchronous) receives event
   ↓
5. Converts to MeetingStartedEvent (domain event)
   ↓
6. MeetingEventHandler.handle() called
   ↓
7. MeetingService.startMeeting() invoked
   ↓
8. Creates/updates Meeting entity
   ↓
9. Creates Session entity with LIVE status
   ↓
10. Commits transaction
```

### Transcript Flow

```
1. Webhook POST /api/webhooks (meeting.transcript)
   ↓
2. WebhookController validates payload
   ↓
3. Publishes MeetingTranscriptWebhookRequest event
   ↓
4. MeetingEventListener (@Async) receives event
   ↓
5. Converts to TranscriptAddedEvent (domain event)
   ↓
6. MeetingEventHandler.handle() called (with retry)
   ↓
7. MeetingService.addTranscript() invoked
   ↓
8. Validates session is LIVE
   ↓
9. Checks idempotency (transcriptId exists?)
   ↓
10. Saves Transcript entity
   ↓
11. Commits transaction
```

### Meeting Ended Flow

```
1. Webhook POST /api/webhooks (meeting.ended)
   ↓
2. WebhookController validates payload
   ↓
3. Publishes MeetingEndedWebhookRequest event
   ↓
4. MeetingEventListener (@Async) receives event
   ↓
5. Converts to MeetingEndedEvent (domain event)
   ↓
6. MeetingEventHandler.handle() called (with retry)
   ↓
7. MeetingService.endMeeting() invoked
   ↓
8. Updates Session status to ENDED
   ↓
9. Sets endedAt timestamp
   ↓
10. Commits transaction
```

## Event-Driven Architecture

### Event Types

1. **Webhook Events** (Infrastructure Layer)
   - `MeetingStartedWebhookRequest`
   - `MeetingTranscriptWebhookRequest`
   - `MeetingEndedWebhookRequest`

2. **Domain Events** (Application Layer)
   - `MeetingStartedEvent`
   - `TranscriptAddedEvent`
   - `MeetingEndedEvent`

### Event Processing Strategy

- **Synchronous**: `meeting.started` events (lower volume, critical path)
- **Asynchronous**: `meeting.transcript` and `meeting.ended` events (higher volume, can tolerate slight delay)

### Benefits of Event-Driven Approach

1. **Decoupling**: Webhook controller doesn't need to know about business logic
2. **Scalability**: Async processing allows handling bursts of traffic
3. **Resilience**: Failed events don't crash the webhook endpoint
4. **Testability**: Events can be mocked and tested independently

## Error Handling & Resilience

### Retry Strategy

**Configuration**:
- Max attempts: 3
- Initial delay: 1000ms
- Multiplier: 2.0 (exponential backoff)
- Max interval: 5000ms

**Retryable Exceptions**:
- `DataAccessException` (database connection issues, transient failures)

**Non-Retryable Exceptions**:
- `IllegalArgumentException` (validation errors)
- `SessionEndedException` (business rule violations)
- `InvalidEventException` (malformed payloads)

### Error Response Format

```json
{
  "timestamp": "2024-12-13T06:57:09.736Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing required field: data.transcriptId",
  "path": "/api/webhooks"
}
```

### Global Exception Handler

All exceptions are caught by `GlobalExceptionHandler` and converted to appropriate HTTP status codes:

- `400 Bad Request`: Validation errors, malformed payloads
- `404 Not Found`: Meeting or session not found
- `409 Conflict`: Duplicate entries, session already ended
- `500 Internal Server Error`: Unexpected errors

### Back-Pressure Handling

- Thread pool executor uses `CallerRunsPolicy`
- When queue is full (100 tasks), caller thread executes the task
- Prevents unbounded queue growth
- Provides natural back-pressure to webhook provider

## Idempotency Strategy

### Transcript Idempotency

**Key**: `transcriptId` (primary key)

**Behavior**:
1. Check if `transcriptId` exists before saving
2. If exists → log and return silently (idempotent)
3. If not exists → save transcript
4. Database constraint as safety net

**Rationale**: Webhook providers may retry failed requests. Idempotency ensures duplicate deliveries don't create duplicate transcripts.

### Session Idempotency

**Key**: `sessionId` (primary key)

**Behavior**:
1. Check if `sessionId` exists
2. If exists and LIVE → update meeting details, return silently (idempotent)
3. If exists and ENDED → throw exception (invalid state)
4. If not exists → create new session

**Rationale**: Multiple `meeting.started` events for the same session should be handled gracefully.

### Meeting Idempotency

**Key**: `meetingId` (primary key)

**Behavior**:
- Multiple sessions can exist for the same meeting (concurrent or sequential)
- Meeting details are updated on each `meeting.started` event
- No idempotency check needed (meeting can have multiple sessions)

## Database Schema

### Meeting Entity

```sql
CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    title VARCHAR NOT NULL,
    room_name VARCHAR NOT NULL,
    organizer_id UUID NOT NULL,
    organizer_name VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

**Relationships**: One-to-many with Sessions

### Session Entity

```sql
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    status VARCHAR NOT NULL, -- 'LIVE' or 'ENDED'
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    reason VARCHAR,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id)
);
```

**Relationships**: 
- Many-to-one with Meeting
- One-to-many with Transcripts

### Transcript Entity

```sql
CREATE TABLE transcripts (
    id UUID PRIMARY KEY, -- transcriptId
    meeting_id UUID NOT NULL,
    session_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    speaker_id UUID NOT NULL,
    speaker_name VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    language VARCHAR,
    start_offset BIGINT NOT NULL, -- Duration in seconds
    end_offset BIGINT NOT NULL,   -- Duration in seconds
    FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
```

**Indexes**:
- `(meeting_id, session_id, sequence_number)` for ordered retrieval

## API Design

### POST /api/webhooks

**Purpose**: Receive webhook events from meeting platform

**Request Body**: JSON payload with event-specific structure

**Response**: 
- Status: `202 Accepted`
- Body: `{"status": "accepted"}`

**Event Types**:
1. `meeting.started`
2. `meeting.transcript`
3. `meeting.ended`

### GET /api/meetings/{meetingId}/sessions/{sessionId}/transcript

**Purpose**: Retrieve ordered transcript for a session

**Response**: Array of transcript segments ordered by `sequenceNumber`

**Example Response**:
```json
[
  {
    "id": "transcript-id-1",
    "meetingId": "meeting-id",
    "sessionId": "session-id",
    "sequenceNumber": 1,
    "speaker": {
      "id": "speaker-id",
      "name": "Alice"
    },
    "content": "Hello, everyone.",
    "startOffset": "PT2S",
    "endOffset": "PT5S",
    "language": "en"
  }
]
```

### GET /api/webhooks/health

**Purpose**: Health check endpoint

**Response**: `{"status": "UP"}`

## Scalability Considerations

### Current Limitations

1. **In-Process Events**: Events are processed within the same JVM
   - **Impact**: Limited horizontal scaling
   - **Mitigation**: Can migrate to Kafka/RabbitMQ for distributed processing

2. **Embedded Database**: H2 database is in-memory
   - **Impact**: Data not persisted, single instance only
   - **Mitigation**: Configure external database (PostgreSQL/MySQL) for production

3. **Thread Pool**: Fixed size (5-10 threads)
   - **Impact**: Limited concurrent processing
   - **Mitigation**: Adjust pool size based on load, consider reactive programming

### Scaling Strategies

1. **Horizontal Scaling**:
   - Deploy multiple instances behind load balancer
   - Use external message broker (Kafka) for event distribution
   - Use shared database (PostgreSQL) for state

2. **Database Optimization**:
   - Add indexes on frequently queried fields
   - Partition transcripts table by session_id
   - Use read replicas for transcript retrieval

3. **Caching**:
   - Cache frequently accessed meetings/sessions
   - Use Redis for session state
   - Cache transcript queries

4. **Async Processing**:
   - Move to dedicated message queue (Kafka/RabbitMQ)
   - Implement dead letter queue for failed events
   - Add event replay capability

## Trade-offs & Decisions

### 1. In-Process Events vs External Message Broker

**Decision**: Started with in-process Spring Application Events

**Trade-offs**:
- ✅ Simple to implement and test
- ✅ No external dependencies
- ✅ Fast processing (no network overhead)
- ❌ Limited horizontal scaling
- ❌ Events lost on application restart
- ❌ No cross-service communication

**Future**: Migrate to Kafka for production scale

### 2. Synchronous vs Asynchronous Processing

**Decision**: Synchronous for `meeting.started`, async for others

**Trade-offs**:
- ✅ Faster response for critical path (started)
- ✅ Better throughput for high-volume events (transcript)
- ❌ Eventual consistency (transcript may arrive before processing completes)
- ❌ More complex error handling

### 3. Idempotency: Check-Before-Save vs Database Constraint

**Decision**: Check before save, database constraint as safety net

**Trade-offs**:
- ✅ Avoids unnecessary database operations
- ✅ Better error messages
- ✅ Database constraint provides safety net
- ❌ Slight performance overhead (extra query)

### 4. Error Handling: Fail Fast vs Retry

**Decision**: Fail fast for validation errors, retry for transient failures

**Trade-offs**:
- ✅ Prevents retrying non-retryable errors
- ✅ Faster feedback for user errors
- ✅ Automatic recovery from transient failures
- ❌ More complex retry configuration

### 5. Database: Embedded H2 vs External Database

**Decision**: Embedded H2 for local development

**Trade-offs**:
- ✅ No external dependencies for local dev
- ✅ Fast startup
- ✅ Easy testing
- ❌ Data not persisted
- ❌ Single instance only
- ❌ Not suitable for production

**Production**: Use PostgreSQL or MySQL with connection pooling

## Future Enhancements

1. **Dead Letter Queue**: Store failed events after max retries
2. **Webhook Signature Verification**: Verify HMAC signatures for security
3. **API Versioning**: Support multiple API versions (`/api/v1/webhooks`)
4. **Structured Logging**: JSON logs with correlation IDs
5. **Metrics & Observability**: Prometheus metrics, distributed tracing
6. **Rate Limiting**: Protect against webhook spam
7. **Event Replay**: Ability to replay events from DLQ
8. **Batch Processing**: Process multiple transcripts in a single transaction
