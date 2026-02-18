package com.github.meeting_platform.domain.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.meeting_platform.common.exceptions.MeetingNotFoundException;
import com.github.meeting_platform.common.exceptions.SessionEndedException;
import com.github.meeting_platform.common.exceptions.SessionNotFoundException;
import com.github.meeting_platform.domain.model.Meeting;
import com.github.meeting_platform.domain.model.Session;
import com.github.meeting_platform.domain.model.Transcript;
import com.github.meeting_platform.domain.repository.MeetingRepository;
import com.github.meeting_platform.domain.repository.SessionRepository;
import com.github.meeting_platform.domain.repository.TranscriptRepository;
import com.github.meeting_platform.domain.service.command.AddTranscriptCommand;
import com.github.meeting_platform.domain.service.command.EndMeetingCommand;
import com.github.meeting_platform.domain.service.command.StartMeetingCommand;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final SessionRepository sessionRepository;
    private final TranscriptRepository transcriptRepository;

    @Override
    @Transactional
    public void startMeeting(StartMeetingCommand cmd) {
        var existingSession = sessionRepository.findById(cmd.getSessionId());
        if (existingSession.isPresent()) {
            var session = existingSession.get();
            if (session.getStatus() == Session.SessionStatus.LIVE) {
                log.debug("Session already exists and is LIVE (idempotent): sessionId={}, meetingId={}",
                        cmd.getSessionId(), cmd.getMeetingId());
                updateMeetingIfExists(cmd);
                return;
            }
            if (session.getStatus() == Session.SessionStatus.ENDED) {
                throw new SessionEndedException(
                        "Cannot start a new session with ID " + cmd.getSessionId() + " - session already ended");
            }
        }

        createOrUpdateMeeting(cmd);
        var session = new Session(cmd.getSessionId(), cmd.getMeetingId(), cmd.getStartedAt());
        sessionRepository.save(session);
        log.debug("Successfully started meeting: meetingId={}, sessionId={}", cmd.getMeetingId(), cmd.getSessionId());
    }

    private void updateMeetingIfExists(StartMeetingCommand cmd) {
        meetingRepository.findById(cmd.getMeetingId()).ifPresent(meeting -> {
            meeting.setTitle(cmd.getTitle());
            meeting.setRoomName(cmd.getRoomName());
            meeting.setOrganizer(new Meeting.Organizer(cmd.getOrganizedById(), cmd.getOrganizedByName()));
            meeting.setCreatedAt(cmd.getCreatedAt());
            meetingRepository.save(meeting);
        });
    }

    private void createOrUpdateMeeting(StartMeetingCommand cmd) {
        var existingMeeting = meetingRepository.findById(cmd.getMeetingId());
        if (existingMeeting.isPresent()) {
            var meeting = existingMeeting.get();
            meeting.setTitle(cmd.getTitle());
            meeting.setRoomName(cmd.getRoomName());
            meeting.setOrganizer(new Meeting.Organizer(cmd.getOrganizedById(), cmd.getOrganizedByName()));
            meeting.setCreatedAt(cmd.getCreatedAt());
            meetingRepository.save(meeting);
        } else {
            var meeting = new Meeting(
                    cmd.getMeetingId(),
                    cmd.getTitle(),
                    cmd.getRoomName(),
                    new Meeting.Organizer(cmd.getOrganizedById(), cmd.getOrganizedByName()),
                    cmd.getCreatedAt());
            meetingRepository.save(meeting);
        }
    }

    @Override
    @Transactional
    public void addTranscript(AddTranscriptCommand cmd) {
        if (meetingRepository.findById(cmd.getMeetingId()).isEmpty()) {
            throw new MeetingNotFoundException("Meeting not found: " + cmd.getMeetingId());
        }

        var session = sessionRepository.findById(cmd.getSessionId())
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + cmd.getSessionId()));

        if (!session.getMeetingId().equals(cmd.getMeetingId())) {
            throw new SessionNotFoundException("Session " + cmd.getSessionId() + " does not belong to meeting " + cmd.getMeetingId());
        }

        if (session.getStatus() == Session.SessionStatus.ENDED) {
            log.info("Adding transcript to ended session (late delivery): sessionId={}, meetingId={}, transcriptId={}",
                    cmd.getSessionId(), cmd.getMeetingId(), cmd.getTranscriptId());
        }

        if (transcriptRepository.findById(cmd.getTranscriptId()).isPresent()) {
            log.debug("Transcript already exists (idempotent): transcriptId={}, sessionId={}, meetingId={}",
                    cmd.getTranscriptId(), cmd.getSessionId(), cmd.getMeetingId());
            return;
        }

        var transcript = new Transcript(
                cmd.getTranscriptId(),
                cmd.getMeetingId(),
                cmd.getSessionId(),
                cmd.getSequenceNumber(),
                cmd.getSpeakerId(),
                cmd.getSpeakerName(),
                cmd.getContent(),
                cmd.getLanguage(),
                cmd.getStartOffset(),
                cmd.getEndOffset());

        try {
            transcriptRepository.save(transcript);
            log.debug("Successfully saved transcript: transcriptId={}, sequenceNumber={}", cmd.getTranscriptId(), cmd.getSequenceNumber());
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate transcript detected during save (idempotent): transcriptId={}, sessionId={}",
                    cmd.getTranscriptId(), cmd.getSessionId());
        }
    }

    @Override
    @Transactional
    public void endMeeting(EndMeetingCommand cmd) {
        if (meetingRepository.findById(cmd.getMeetingId()).isEmpty()) {
            throw new MeetingNotFoundException("Meeting not found: " + cmd.getMeetingId());
        }

        var session = sessionRepository.findById(cmd.getSessionId())
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + cmd.getSessionId()));

        if (!session.getMeetingId().equals(cmd.getMeetingId())) {
            throw new SessionNotFoundException("Session " + cmd.getSessionId() + " does not belong to meeting " + cmd.getMeetingId());
        }

        if (session.getEndedAt() != null) {
            throw new SessionEndedException("Session already ended: " + cmd.getSessionId());
        }

        session.end(cmd.getEndedAt(), cmd.getReason());
        sessionRepository.save(session);
    }

    @Override
    public List<Transcript> getSessionTranscripts(UUID meetingId, UUID sessionId) {
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new MeetingNotFoundException("Meeting not found: " + meetingId);
        }

        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        if (!session.getMeetingId().equals(meetingId)) {
            throw new SessionNotFoundException("Session " + sessionId + " does not belong to meeting " + meetingId);
        }

        return StreamSupport.stream(
                transcriptRepository.findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(meetingId, sessionId).spliterator(),
                false
        ).toList();
    }
}
