package com.github.meeting_platform.domain.model;

import java.time.Duration;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transcripts", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "meetingId", "sessionId", "sequenceNumber" })
})
@Getter
@NoArgsConstructor
public class Transcript {

    @Id
    private UUID id;

    @NotNull(message = "Transcript must be associated with a meeting")
    private UUID meetingId;

    @NotNull(message = "Transcript must be associated with a session")
    private UUID sessionId;

    @NotNull(message = "Sequence number cannot be null")
    private Integer sequenceNumber;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "speaker_id")),
            @AttributeOverride(name = "name", column = @Column(name = "speaker_name"))
    })
    @NotNull(message = "Transcript must have a speaker")
    private Speaker speaker;

    @NotNull(message = "Transcript content cannot be null")
    @Column(columnDefinition = "TEXT")
    private String content;

    private String language;

    @NotNull(message = "Transcript must have a start offset")
    private Duration startOffset;

    @NotNull(message = "Transcript must have an end offset")
    private Duration endOffset;

    public Transcript(UUID id,
            UUID meetingId,
            UUID sessionId,
            Integer sequenceNumber,
            UUID speakerId,
            String speakerName,
            String content,
            String language,
            Duration startOffset,
            Duration endOffset) {

        this.id = id;
        this.meetingId = meetingId;
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
        this.speaker = new Speaker(speakerId, speakerName);
        this.content = content;
        this.language = language;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    public static class Speaker {

        private UUID id;
        private String name;

        public Speaker(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
