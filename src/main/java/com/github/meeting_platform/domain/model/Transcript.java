package com.github.meeting_platform.domain.model;

import java.time.Duration;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transcripts")
@Getter
@NoArgsConstructor
public class Transcript {

    @Id
    private UUID id;

    private UUID meetingId;
    private UUID sessionId;
    private Integer sequenceNumber;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "speaker_id")),
            @AttributeOverride(name = "name", column = @Column(name = "speaker_name"))
    })
    private Speaker speaker;

    private String content;

    private String language;

    private Duration startOffset;
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
