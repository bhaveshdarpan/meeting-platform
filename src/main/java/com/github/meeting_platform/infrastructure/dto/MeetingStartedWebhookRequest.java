package com.github.meeting_platform.infrastructure.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class MeetingStartedWebhookRequest {

    private String event;
    private Meeting meeting;

    @Data
    public static class Meeting {
        private UUID id;
        private UUID sessionId;
        private String title;
        private String roomName;
        private String status;
        private Instant createdAt;
        private Instant startedAt;
        private OrganizedBy organizedBy;
    }

    @Data
    public static class OrganizedBy {
        private UUID id;
        private String name;
    }
}
