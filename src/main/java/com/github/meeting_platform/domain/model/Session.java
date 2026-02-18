package com.github.meeting_platform.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sessions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "id", "meetingId" })
})
@Getter
@NoArgsConstructor
public class Session {

    @Id
    private UUID id;

    @NotNull(message = "Meeting ID cannot be null")
    private UUID meetingId;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Session status cannot be null")
    private SessionStatus status;

    @NotNull(message = "Session start time cannot be null")
    private Instant startedAt;
    private Instant endedAt;
    private String reason;

    public Session(UUID id, UUID meetingId, Instant startedAt) {
        this.id = id;
        this.meetingId = meetingId;
        this.startedAt = startedAt;
        this.status = SessionStatus.LIVE;
    }

    public void end(Instant endedAt, String reason) {
        this.status = SessionStatus.ENDED;
        this.endedAt = endedAt;
        this.reason = reason;
    }

    public enum SessionStatus {
        LIVE, ENDED
    }
}
