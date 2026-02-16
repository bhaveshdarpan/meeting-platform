package com.github.meeting_platform.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor
public class Session {

    @Id
    private UUID id;

    private UUID meetingId;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

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
