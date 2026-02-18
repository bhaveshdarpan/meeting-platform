package com.github.meeting_platform.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.meeting_platform.domain.model.Session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findById(@NotNull UUID sessionId);

    Optional<Session> findActiveByMeetingId(@NotNull UUID meetingId);

    <S extends Session> S save(@NotNull @Valid S session);
}
