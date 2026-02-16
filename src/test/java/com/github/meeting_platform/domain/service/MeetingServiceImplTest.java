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

import com.github.meeting_platform.domain.model.Meeting;
import com.github.meeting_platform.domain.model.Session;
import com.github.meeting_platform.domain.model.Transcript;
import com.github.meeting_platform.domain.repository.MeetingRepository;
import com.github.meeting_platform.domain.repository.SessionRepository;
import com.github.meeting_platform.domain.repository.TranscriptRepository;

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

    // ============================================================
    // START MEETING TESTS
    // ============================================================

    @Nested
    class StartMeetingTests {

        @Test
        void shouldCreateMeetingWhenMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

            meetingService.startMeeting(
                    meetingId,
                    sessionId,
                    "Title",
                    "Room",
                    UUID.randomUUID(),
                    "Org",
                    Instant.now().toString(),
                    Instant.now().toString());

            verify(meetingRepository).save(any(Meeting.class));
            verify(sessionRepository).save(any(Session.class));
        }

        @Test
        void shouldUpdateMeetingWhenExists() {
            Meeting existing = new Meeting(meetingId, "old", "oldRoom", null, Instant.now());
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(existing));

            meetingService.startMeeting(
                    meetingId,
                    sessionId,
                    "newTitle",
                    "newRoom",
                    UUID.randomUUID(),
                    "org",
                    Instant.now().toString(),
                    Instant.now().toString());

            ArgumentCaptor<Meeting> cap = ArgumentCaptor.forClass(Meeting.class);
            verify(meetingRepository).save(cap.capture());

            assertEquals("newTitle", cap.getValue().getTitle());
            assertEquals("newRoom", cap.getValue().getRoomName());
        }

        @Test
        void shouldPropagateWhenSessionSaveFails() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());
            when(sessionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThrows(RuntimeException.class, () -> meetingService.startMeeting(
                    meetingId,
                    sessionId,
                    "Title",
                    "Room",
                    UUID.randomUUID(),
                    "Org",
                    Instant.now().toString(),
                    Instant.now().toString()));
        }
    }

    // ============================================================
    // ADD TRANSCRIPT TESTS
    // ============================================================

    @Nested
    class AddTranscriptTests {

        @Test
        void shouldThrowWhenMeetingMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> meetingService.addTranscript(
                    meetingId, sessionId, UUID.randomUUID(), 1,
                    UUID.randomUUID(), "speaker", "content",
                    Duration.ZERO, Duration.ZERO, "en"));
        }

        @Test
        void shouldThrowWhenNoActiveSession() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(new Meeting()));
            when(sessionRepository.findActiveByMeetingId(meetingId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> meetingService.addTranscript(
                    meetingId, sessionId, UUID.randomUUID(), 1,
                    UUID.randomUUID(), "speaker", "content",
                    Duration.ZERO, Duration.ZERO, "en"));
        }

        @Test
        void shouldThrowWhenTranscriptAlreadyExists() {
            UUID transcriptId = UUID.randomUUID();

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(new Meeting()));
            when(sessionRepository.findActiveByMeetingId(meetingId)).thenReturn(Optional.of(new Session()));
            when(transcriptRepository.findById(transcriptId)).thenReturn(Optional.of(new Transcript()));

            assertThrows(IllegalArgumentException.class, () -> meetingService.addTranscript(
                    meetingId, sessionId, transcriptId, 1,
                    UUID.randomUUID(), "speaker", "content",
                    Duration.ZERO, Duration.ZERO, "en"));

            verify(transcriptRepository, never()).save(any());
        }

        @Test
        void shouldPersistTranscriptWhenValid() {
            UUID transcriptId = UUID.randomUUID();
            UUID speakerId = UUID.randomUUID();

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(new Meeting()));
            when(sessionRepository.findActiveByMeetingId(meetingId)).thenReturn(Optional.of(new Session()));
            when(transcriptRepository.findById(transcriptId)).thenReturn(Optional.empty());

            meetingService.addTranscript(
                    meetingId,
                    sessionId,
                    transcriptId,
                    5,
                    speakerId,
                    "Alice",
                    "Hello world",
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(3),
                    "en");

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
        void shouldThrowWhenSessionMissing() {
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> meetingService.endMeeting(meetingId, sessionId, Instant.now(), "reason"));
        }

        @Test
        void shouldThrowWhenSessionBelongsToDifferentMeeting() {
            Session session = new Session(sessionId, UUID.randomUUID(), Instant.now());
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            assertThrows(IllegalArgumentException.class,
                    () -> meetingService.endMeeting(meetingId, sessionId, Instant.now(), "reason"));
        }

        @Test
        void shouldUpdateSessionStatusAndEndTime() {
            Session session = new Session(sessionId, meetingId, Instant.now());
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            Instant endTime = Instant.now();

            meetingService.endMeeting(meetingId, sessionId, endTime, "done");

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

            assertThrows(IllegalArgumentException.class,
                    () -> meetingService.getSessionTranscripts(meetingId, sessionId));
        }

        @Test
        void shouldThrowWhenSessionMissing() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(new Meeting()));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> meetingService.getSessionTranscripts(meetingId, sessionId));
        }

        @Test
        void shouldReturnEmptyListWhenNoneFound() {
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(new Meeting()));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(new Session()));
            when(transcriptRepository.findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(meetingId, sessionId))
                    .thenReturn(List.of());

            List<Transcript> result = meetingService.getSessionTranscripts(meetingId, sessionId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnOrderedTranscripts() {
            Transcript t = new Transcript(
                    UUID.randomUUID(), meetingId, sessionId, 1,
                    UUID.randomUUID(), "speaker", "content",
                    "en", Duration.ZERO, Duration.ZERO);

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(new Meeting()));
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(new Session()));
            when(transcriptRepository.findByMeetingIdAndSessionIdOrderBySequenceNumberAsc(meetingId, sessionId))
                    .thenReturn(List.of(t));

            List<Transcript> result = meetingService.getSessionTranscripts(meetingId, sessionId);

            assertEquals(1, result.size());
            assertEquals(t.getId(), result.get(0).getId());
        }
    }
}
