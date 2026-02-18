package com.github.meeting_platform.domain.service.command;

import java.time.Instant;
import java.util.UUID;

import lombok.Value;

@Value
public class EndMeetingCommand {
    UUID meetingId;
    UUID sessionId;
    Instant endedAt;
    String reason;
}
