package com.github.meeting_platform.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.github.meeting_platform.domain.repository.MeetingRepository;
import com.github.meeting_platform.domain.repository.SessionRepository;
import com.github.meeting_platform.domain.repository.TranscriptRepository;
import com.github.meeting_platform.domain.model.Meeting;
import com.github.meeting_platform.domain.model.Session;
import com.github.meeting_platform.domain.model.Transcript;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final SessionRepository sessionRepository;
    private final TranscriptRepository transcriptRepository;

    @Override
    public void startMeeting(UUID meetingId, UUID sessionId, String title, String roomName, UUID organizedById,
            String organizedByName, String createdAt, String startedAt) {
        if (meetingRepository.findById(meetingId).isPresent()) {
            // update meeting details and start a new session
            Meeting meeting = meetingRepository.findById(meetingId).get();
            meeting.setTitle(title);
            meeting.setRoomName(roomName);
            meeting.setOrganizer(new Meeting.Organizer(organizedById, organizedByName));
            meetingRepository.save(meeting);
            Instant startedAtInstant = Instant.parse(startedAt);
            Session session = new Session(sessionId, meetingId, startedAtInstant);
            sessionRepository.save(session);
            return;
        }
        Instant createdAtInstant = Instant.parse(createdAt);
        Meeting meeting = new Meeting(meetingId, title, roomName, new Meeting.Organizer(organizedById, organizedByName),
                createdAtInstant);
        meetingRepository.save(meeting);
        Instant startedAtInstant = Instant.parse(startedAt);
        Session session = new Session(sessionId, meetingId, startedAtInstant);
        sessionRepository.save(session);
    }

    @Override
    public void addTranscript(UUID meetingId, UUID sessionId, UUID transcriptId, int sequenceNumber,
            UUID speakerId, String speakerName, String content,
            Duration startOffset, Duration endOffset, String language) {
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new IllegalArgumentException("Meeting not found: " + meetingId);
        } else if (sessionRepository.findById(sessionId).isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        } else if (sessionRepository.findById(sessionId).get().getMeetingId() != meetingId) {
            throw new IllegalArgumentException("Session " + sessionId + " does not belong to meeting " + meetingId);
        } else if (transcriptRepository.findById(transcriptId).isPresent()) {
            throw new IllegalArgumentException("Transcript already exists: " + transcriptId);
        }
        Transcript transcript = new Transcript(transcriptId, meetingId, sessionId, sequenceNumber,
                speakerId, speakerName, content, language, startOffset, endOffset);
        transcriptRepository.save(transcript);
    }

    @Override
    public void endMeeting(UUID meetingId, UUID sessionId, Instant endedAt, String reason) {
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new IllegalArgumentException("Meeting not found: " + meetingId);
        } else if (sessionRepository.findById(sessionId).isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        } else if (sessionRepository.findById(sessionId).get().getMeetingId() != meetingId) {
            throw new IllegalArgumentException("Session " + sessionId + " does not belong to meeting " + meetingId);
        } else if (sessionRepository.findById(sessionId).get().getEndedAt() != null) {
            throw new IllegalArgumentException("Session already ended: " + sessionId);
        }
        Session session = sessionRepository.findById(sessionId).get();
        session.end(endedAt, reason);
        sessionRepository.save(session);
    }

    public List<Transcript> getSessionTranscripts(UUID meetingId, UUID sessionId) {
        // Validate meeting and session existence
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new IllegalArgumentException("Meeting not found: " + meetingId);
        } else if (sessionRepository.findById(sessionId).isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        } else if (sessionRepository.findById(sessionId).get().getMeetingId() != meetingId) {
            throw new IllegalArgumentException("Session " + sessionId + " does not belong to meeting " + meetingId);
        }
        // Fetch and return transcripts for the given meeting and session in the order
        // of sequence number
        return StreamSupport
                .stream(transcriptRepository.findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(meetingId, sessionId)
                        .spliterator(), false)
                .toList();
    }
}
