package com.github.meeting_platform.infrastructure.dto;

import java.time.Duration;
import java.util.UUID;

import lombok.Data;

@Data
public class MeetingTranscriptWebhookRequest {
    private String event;
    private Meeting meeting;
    private TranscriptData data;

    @Data
    public static class Meeting {
        private UUID id;
        private UUID sessionId;
    }

    @Data
    public static class TranscriptData {
        private UUID transcriptId;
        private int sequenceNumber;
        private Speaker speaker;
        private String content;
        private Duration startOffset;

        private Duration endOffset;

        private String language;
    }

    @Data
    public static class Speaker {
        private UUID id;
        private String name;
    }
}
