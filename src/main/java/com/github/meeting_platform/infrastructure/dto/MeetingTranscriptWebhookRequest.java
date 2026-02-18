package com.github.meeting_platform.infrastructure.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MeetingTranscriptWebhookRequest {
    private String event;
    private Meeting meeting;
    private TranscriptData data;

    @Data
    public static class Meeting {
        @NotNull(message = "Meeting ID cannot be null")
        private UUID id;
        @NotNull(message = "Session ID cannot be null")
        private UUID sessionId;
    }

    @Data
    public static class TranscriptData {
        @NotNull(message = "Transcript ID cannot be null")
        private UUID transcriptId;
        @NotNull(message = "Sequence number cannot be null")
        private int sequenceNumber;
        @NotNull(message = "Speaker cannot be null")
        private Speaker speaker;
        @NotEmpty(message = "Transcript content cannot be empty")
        private String content;
        @NotNull(message = "Start offset cannot be null")
        private Integer startOffset;
        @NotNull(message = "End offset cannot be null")
        private Integer endOffset;
        @NotBlank(message = "Language cannot be blank")
        private String language;
    }

    @Data
    public static class Speaker {
        @NotNull(message = "Speaker ID cannot be null")
        private UUID id;
        @NotBlank(message = "Speaker name cannot be blank")
        private String name;
    }
}
