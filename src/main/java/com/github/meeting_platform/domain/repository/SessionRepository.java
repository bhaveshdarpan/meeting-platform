package com.github.meeting_platform.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.meeting_platform.domain.model.Session;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findById(UUID sessionId);

    Optional<Session> findActiveByMeetingId(UUID meetingId);

    <S extends Session> S save(S session);
}
