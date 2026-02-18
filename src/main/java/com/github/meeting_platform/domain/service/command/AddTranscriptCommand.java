package com.github.meeting_platform.domain.service.command;

import java.time.Duration;
import java.util.UUID;

import lombok.Value;

@Value
public class AddTranscriptCommand {
    UUID meetingId;
    UUID sessionId;
    UUID transcriptId;
    int sequenceNumber;
    UUID speakerId;
    String speakerName;
    String content;
    Duration startOffset;
    Duration endOffset;
    String language;
}
