package com.github.meeting_platform.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Title cannot be null")
    private String title;

    @NotNull(message = "Room name cannot be null")
    private String roomName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "organizer_id")),
            @AttributeOverride(name = "name", column = @Column(name = "organizer_name"))
    })
    @NotNull(message = "Organizer cannot be null")
    private Organizer organizer;

    @NotNull(message = "Meeting creation time cannot be null")
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
