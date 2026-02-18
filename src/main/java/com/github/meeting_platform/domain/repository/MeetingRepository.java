package com.github.meeting_platform.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.meeting_platform.domain.model.Meeting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    Optional<Meeting> findById(@NotNull UUID id);

    <S extends Meeting> S save(@NotNull @Valid S meeting);
}
