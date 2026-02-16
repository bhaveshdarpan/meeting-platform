package com.github.meeting_platform.application.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

@Value
public class MeetingStartedEvent {
    UUID id;
    UUID sessionId;
    String title;
    String roomName;
    String status;
    Instant createdAt;
    Instant startedAt;
    UUID organizedById;
    String organizedByName;
}
