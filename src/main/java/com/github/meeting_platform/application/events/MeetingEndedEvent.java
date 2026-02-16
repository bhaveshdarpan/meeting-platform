package com.github.meeting_platform.application.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

@Value
public class MeetingEndedEvent {
    UUID id;
    UUID sessionId;
    String title;
    // String roomName;
    String status;
    Instant createdAt;
    Instant startedAt;
    Instant endedAt;
    UUID organizedById;
    String organizedByName;
    String reason;
}
