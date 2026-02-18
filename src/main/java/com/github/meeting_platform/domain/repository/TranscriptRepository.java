package com.github.meeting_platform.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.meeting_platform.domain.model.Transcript;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    Optional<Transcript> findById(@NotNull UUID id);

    <S extends Transcript> S save(@NotNull @Valid S transcript);

    Iterable<Transcript> findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(@NotNull UUID meetingId,
            @NotNull UUID sessionId);
}
