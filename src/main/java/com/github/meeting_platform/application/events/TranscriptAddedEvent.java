package com.github.meeting_platform.application.events;

import java.time.Duration;
import java.util.UUID;
import lombok.Value;

@Value
public class TranscriptAddedEvent {
    UUID id;
    UUID meetingId;
    UUID sessionId;
    int sequenceNumber;
    UUID speakerId;
    String speakerName;
    String content;
    Duration startOffset;
    Duration endOffset;
    String language;
}
