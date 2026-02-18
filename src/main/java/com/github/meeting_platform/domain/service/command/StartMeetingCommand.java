package com.github.meeting_platform.domain.service.command;

import java.time.Instant;
import java.util.UUID;

import lombok.Value;

@Value
public class StartMeetingCommand {
    UUID meetingId;
    UUID sessionId;
    String title;
    String roomName;
    UUID organizedById;
    String organizedByName;
    Instant createdAt;
    Instant startedAt;
}
