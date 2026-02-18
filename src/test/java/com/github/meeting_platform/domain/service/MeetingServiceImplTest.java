package com.github.meeting_platform.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    TranscriptRepository transcriptRepository;

    @InjectMocks
    MeetingServiceImpl meetingService;

    UUID meetingId;
    UUID sessionId;

    @BeforeEach
    void setup() {
        meetingId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    private static Meeting createMeeting(UUID id) {
        Meeting m = new Meeting();
        m.setId(id);
        m.setTitle("Test");
        m.setRoomName("Room");
        m.setOrganizer(new Meeting.Organizer(UUID.randomUUID(), "Org"));
        m.setCreatedAt(Instant.now());
        return m;
    }

    private static Session createSession(UUID sessionId, UUID meetingId) {
        return new Session(sessionId, meetingId, Instant.now());
    }

    private static Transcript createTranscript(UUID id) {
        return new Transcript(id, UUID.randomUUID(), UUID.randomUUID(), 1,
                UUID.randomUUID(), "speaker", "content", "en",
                Duration.ZERO, Duration.ZERO);
    }

    private static StartMeetingCommand startCommand(UUID meetingId, UUID sessionId) {
        return new StartMeetingCommand(
                meetingId,
                sessionId,
                "Title",
                "Room",
                UUID.randomUUID(),
                "Org",
                Instant.now(),
                Instant.now());
    }

    // ============================================================
    // START MEETING TESTS
    // ============================================================

    @Nested
    class StartMeetingTests {

        @Test
        void shouldCreateMeetingWhenMissing() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

            meetingService.startMeeting(startCommand(meetingId, sessionId));

            verify(meetingRepository).save(any(Meeting.class));
            verify(sessionRepository).save(any(Session.class));
        }

        @Test
        void shouldUpdateMeetingWhenExists() {
            Meeting existing = new Meeting(meetingId, "old", "oldRoom", null, Instant.now());
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(existing));

            meetingService.startMeeting(new StartMeetingCommand(
                    meetingId, sessionId, "newTitle", "newRoom",
                    UUID.randomUUID(), "org", Instant.now(), Instant.now()));

            ArgumentCaptor<Meeting> cap = ArgumentCaptor.forClass(Meeting.class);
            verify(meetingRepository).save(cap.capture());

            assertEquals("newTitle", cap.getValue().getTitle());
            assertEquals("newRoom", cap.getValue().getRoomName());
        }

        @Test
        void shouldPropagateWhenSessionSaveFails() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());
            when(sessionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () ->
                    meetingService.startMeeting(startCommand(meetingId, sessionId)));
        }

        @Test
        void shouldThrowSessionEndedWhenSessionAlreadyEnded() {
            Session endedSession = createSession(sessionId, meetingId);
            endedSession.end(Instant.now(), "test");
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(endedSession));

            assertThrows(SessionEndedException.class, () ->
                    meetingService.startMeeting(startCommand(meetingId, sessionId)));
        }
    }

    // ============================================================
    // ADD TRANSCRIPT TESTS
    // ============================================================

    @Nested
    class AddTranscriptTests {

        @Test
        void shouldThrowWhenMeetingMissingForAddTranscript() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

            assertThrows(MeetingNotFoundException.class, () -> meetingService.addTranscript(
                    new AddTranscriptCommand(meetingId, sessionId, UUID.randomUUID(), 1,
                            UUID.randomUUID(), "speaker", "content",
                            Duration.ZERO, Duration.ZERO, "en")));
        }

        @Test
        void shouldThrowWhenSessionMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class, () -> meetingService.addTranscript(
                    new AddTranscriptCommand(meetingId, sessionId, UUID.randomUUID(), 1,
                            UUID.randomUUID(), "speaker", "content",
                            Duration.ZERO, Duration.ZERO, "en")));
        }

        @Test
        void shouldNotSaveDuplicateTranscriptIdempotent() {
            UUID transcriptId = UUID.randomUUID();
            Session session = createSession(sessionId, meetingId);

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(transcriptRepository.findById(transcriptId)).thenReturn(Optional.of(createTranscript(transcriptId)));

            meetingService.addTranscript(new AddTranscriptCommand(
                    meetingId, sessionId, transcriptId, 1,
                    UUID.randomUUID(), "speaker", "content",
                    Duration.ZERO, Duration.ZERO, "en"));

            verify(transcriptRepository, never()).save(any());
        }

        @Test
        void shouldPersistTranscriptWhenValid() {
            UUID transcriptId = UUID.randomUUID();
            UUID speakerId = UUID.randomUUID();
            Session session = createSession(sessionId, meetingId);

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(transcriptRepository.findById(transcriptId)).thenReturn(Optional.empty());

            meetingService.addTranscript(new AddTranscriptCommand(
                    meetingId,
                    sessionId,
                    transcriptId,
                    5,
                    speakerId,
                    "Alice",
                    "Hello world",
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(3),
                    "en"));

            ArgumentCaptor<Transcript> captor = ArgumentCaptor.forClass(Transcript.class);
            verify(transcriptRepository).save(captor.capture());

            Transcript saved = captor.getValue();
            assertEquals(5, saved.getSequenceNumber());
            assertEquals("Hello world", saved.getContent());
            assertEquals("Alice", saved.getSpeaker().getName());
        }
    }

    // ============================================================
    // END MEETING TESTS
    // ============================================================

    @Nested
    class EndMeetingTests {

        @Test
        void shouldThrowWhenMeetingMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

            assertThrows(MeetingNotFoundException.class, () ->
                    meetingService.endMeeting(new EndMeetingCommand(meetingId, sessionId, Instant.now(), "reason")));
        }

        @Test
        void shouldThrowWhenSessionMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class, () ->
                    meetingService.endMeeting(new EndMeetingCommand(meetingId, sessionId, Instant.now(), "reason")));
        }

        @Test
        void shouldThrowWhenSessionBelongsToDifferentMeeting() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            Session session = createSession(sessionId, UUID.randomUUID());
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(SessionNotFoundException.class, () ->
                    meetingService.endMeeting(new EndMeetingCommand(meetingId, sessionId, Instant.now(), "reason")));
        }

        @Test
        void shouldThrowWhenSessionAlreadyEnded() {
            Session session = createSession(sessionId, meetingId);
            session.end(Instant.now(), "previous");
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(SessionEndedException.class, () ->
                    meetingService.endMeeting(new EndMeetingCommand(meetingId, sessionId, Instant.now(), "reason")));
        }

        @Test
        void shouldUpdateSessionStatusAndEndTime() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            Session session = createSession(sessionId, meetingId);
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            Instant endTime = Instant.now();

            meetingService.endMeeting(new EndMeetingCommand(meetingId, sessionId, endTime, "done"));

            ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
            verify(sessionRepository).save(captor.capture());

            assertEquals(Session.SessionStatus.ENDED, captor.getValue().getStatus());
            assertEquals(endTime, captor.getValue().getEndedAt());
        }
    }

    // ============================================================
    // GET TRANSCRIPTS TESTS
    // ============================================================

    @Nested
    class GetSessionTranscriptsTests {

        @Test
        void shouldThrowWhenMeetingMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

            assertThrows(MeetingNotFoundException.class, () ->
                    meetingService.getSessionTranscripts(meetingId, sessionId));
        }

        @Test
        void shouldThrowWhenSessionMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(SessionNotFoundException.class, () ->
                    meetingService.getSessionTranscripts(meetingId, sessionId));
        }

        @Test
        void shouldReturnEmptyListWhenNoneFound() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(createSession(sessionId, meetingId)));
            when(transcriptRepository.findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(meetingId, sessionId))
                    .thenReturn(List.of());

            List<Transcript> result = meetingService.getSessionTranscripts(meetingId, sessionId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnOrderedTranscripts() {
            Transcript t = createTranscript(UUID.randomUUID());

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(createMeeting(meetingId)));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(createSession(sessionId, meetingId)));
            when(transcriptRepository.findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(meetingId, sessionId))
                    .thenReturn(List.of(t));

            List<Transcript> result = meetingService.getSessionTranscripts(meetingId, sessionId);

            assertEquals(1, result.size());
            assertEquals(t.getId(), result.get(0).getId());
        }
    }
}
