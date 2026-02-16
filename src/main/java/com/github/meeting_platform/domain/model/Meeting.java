package com.github.meeting_platform.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    private UUID id;

    private String title;

    private String roomName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "organizer_id")),
            @AttributeOverride(name = "name", column = @Column(name = "organizer_name"))
    })
    private Organizer organizer;

    private Instant createdAt;

    @Embeddable
    @Getter
    @NoArgsConstructor
    public static class Organizer {

        private UUID id;
        private String name;

        public Organizer(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
