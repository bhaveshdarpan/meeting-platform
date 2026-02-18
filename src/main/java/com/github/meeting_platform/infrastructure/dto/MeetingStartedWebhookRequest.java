package com.github.meeting_platform.infrastructure.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MeetingStartedWebhookRequest {

    private String event;
    @NotNull(message = "Meeting information cannot be null")
    private Meeting meeting;

    @Data
    public static class Meeting {

        @NotNull(message = "Meeting ID cannot be null")
        private UUID id;
        @NotNull(message = "Session ID cannot be null")
        private UUID sessionId;

        private String title;
        @NotBlank(message = "Meeting room name cannot be blank")
        private String roomName;

        @NotBlank(message = "Meeting status cannot be blank")
        private String status;

        @NotNull(message = "Meeting created time cannot be null")
        private Instant createdAt;

        @NotNull(message = "Meeting started time cannot be null")
        private Instant startedAt;

        @NotNull(message = "Meeting must have an organizer")
        private OrganizedBy organizedBy;
    }

    @Data
    public static class OrganizedBy {
        @NotNull(message = "Organizer ID cannot be null")
        private UUID id;
        @NotBlank(message = "Organizer name cannot be blank")
        private String name;
    }
}
