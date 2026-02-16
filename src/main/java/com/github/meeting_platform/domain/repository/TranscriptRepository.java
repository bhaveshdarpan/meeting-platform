package com.github.meeting_platform.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.meeting_platform.domain.model.Transcript;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    Optional<Transcript> findById(UUID id);
    <S extends Transcript> S save(S transcript);
    Iterable<Transcript> findByMeetingIdAndSessionIdOrderedBySequenceNumber(UUID meetingId, UUID sessionId);
}
