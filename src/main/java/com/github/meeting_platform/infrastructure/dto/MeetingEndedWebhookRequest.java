package com.github.meeting_platform.infrastructure.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class MeetingEndedWebhookRequest {

    private String event;
    private Meeting meeting;
    private String reason;

    @Data
    public static class Meeting {
        private UUID id;
        private UUID sessionId;
        private String title;
        private String status;
        private Instant createdAt;
        private Instant startedAt;
        private Instant endedAt;
        private OrganizedBy organizedBy;
    }

    @Data
    public static class OrganizedBy {
        private UUID id;
        private String name;
    }
}
