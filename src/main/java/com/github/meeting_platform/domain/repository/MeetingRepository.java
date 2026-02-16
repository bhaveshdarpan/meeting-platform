package com.github.meeting_platform.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.meeting_platform.domain.model.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    Optional<Meeting> findById(UUID id);

    <S extends Meeting> S save(S meeting);
}
