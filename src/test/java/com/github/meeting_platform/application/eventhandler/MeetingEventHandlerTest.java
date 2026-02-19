package com.github.meeting_platform.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.meeting_platform.domain.events.MeetingEndedEvent;
import com.github.meeting_platform.domain.events.MeetingStartedEvent;
import com.github.meeting_platform.domain.events.TranscriptAddedEvent;
import com.github.meeting_platform.domain.service.MeetingService;
import com.github.meeting_platform.domain.service.command.AddTranscriptCommand;
import com.github.meeting_platform.domain.service.command.EndMeetingCommand;
import com.github.meeting_platform.domain.service.command.StartMeetingCommand;

@ExtendWith(MockitoExtension.class)
class MeetingEventHandlerTest {

    @Mock
    MeetingService meetingService;

    @InjectMocks
    MeetingEventHandler handler;

    // ============================================================
    // MEETING STARTED TESTS
    // ============================================================

    @Nested
    class MeetingStartedTests {

        @Test
        void shouldCallStartMeetingWithCorrectCommand() {
            UUID meetingId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID organizer = UUID.randomUUID();
            Instant started = Instant.now();
            Instant created = Instant.now();

            MeetingStartedEvent event = new MeetingStartedEvent(
                    meetingId,
                    sessionId,
                    "title",
                    "room",
                    "source",
                    created,
                    started,
                    organizer,
                    "org");

            handler.handle(event);

            ArgumentCaptor<StartMeetingCommand> captor = ArgumentCaptor.forClass(StartMeetingCommand.class);
            verify(meetingService).startMeeting(captor.capture());

            StartMeetingCommand cmd = captor.getValue();
            assertEquals(meetingId, cmd.getMeetingId());
            assertEquals(sessionId, cmd.getSessionId());
            assertEquals("title", cmd.getTitle());
            assertEquals("room", cmd.getRoomName());
            assertEquals(organizer, cmd.getOrganizedById());
            assertEquals("org", cmd.getOrganizedByName());
            assertEquals(created, cmd.getCreatedAt());
            assertEquals(started, cmd.getStartedAt());
        }

        @Test
        void shouldHandleMultipleInvocations() {
            UUID meetingId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();

            MeetingStartedEvent event = new MeetingStartedEvent(
                    meetingId,
                    sessionId,
                    "title",
                    "room",
                    "source",
                    Instant.now(),
                    Instant.now(),
                    UUID.randomUUID(),
                    "org");

            handler.handle(event);
            handler.handle(event);

            verify(meetingService, times(2)).startMeeting(any(StartMeetingCommand.class));
        }

        @Test
        void shouldPropagateExceptionFromService() {
            MeetingStartedEvent event = new MeetingStartedEvent(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "title",
                    "room",
                    "source",
                    Instant.now(),
                    Instant.now(),
                    UUID.randomUUID(),
                    "org");

            doThrow(new RuntimeException("DB error"))
                    .when(meetingService)
                    .startMeeting(any(StartMeetingCommand.class));

            assertThrows(RuntimeException.class, () -> handler.handle(event));
        }
    }

    // ============================================================
    // TRANSCRIPT ADDED TESTS
    // ============================================================

    @Nested
    class TranscriptAddedTests {

        @Test
        void shouldCallAddTranscriptWithCorrectCommand() {
            UUID transcriptId = UUID.randomUUID();
            UUID meetingId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID speakerId = UUID.randomUUID();

            TranscriptAddedEvent event = new TranscriptAddedEvent(
                    transcriptId,
                    meetingId,
                    sessionId,
                    5,
                    speakerId,
                    "Alice",
                    "Hello world",
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(3),
                    "en");

            handler.handle(event);

            ArgumentCaptor<AddTranscriptCommand> captor = ArgumentCaptor.forClass(AddTranscriptCommand.class);
            verify(meetingService).addTranscript(captor.capture());

            AddTranscriptCommand cmd = captor.getValue();
            assertEquals(meetingId, cmd.getMeetingId());
            assertEquals(sessionId, cmd.getSessionId());
            assertEquals(transcriptId, cmd.getTranscriptId());
            assertEquals(5, cmd.getSequenceNumber());
            assertEquals(speakerId, cmd.getSpeakerId());
            assertEquals("Alice", cmd.getSpeakerName());
            assertEquals("Hello world", cmd.getContent());
            assertEquals(Duration.ofSeconds(1), cmd.getStartOffset());
            assertEquals(Duration.ofSeconds(3), cmd.getEndOffset());
            assertEquals("en", cmd.getLanguage());
        }

        @Test
        void shouldAllowNullSpeakerName() {
            TranscriptAddedEvent event = new TranscriptAddedEvent(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1,
                    UUID.randomUUID(),
                    null,
                    "content",
                    Duration.ZERO,
                    Duration.ZERO,
                    "en");

            handler.handle(event);

            ArgumentCaptor<AddTranscriptCommand> captor = ArgumentCaptor.forClass(AddTranscriptCommand.class);
            verify(meetingService).addTranscript(captor.capture());
            assertNull(captor.getValue().getSpeakerName());
        }

        @Test
        void shouldPropagateExceptionFromService() {
            TranscriptAddedEvent event = new TranscriptAddedEvent(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1,
                    UUID.randomUUID(),
                    "speaker",
                    "content",
                    Duration.ZERO,
                    Duration.ZERO,
                    "en");

            doThrow(new RuntimeException("DB error"))
                    .when(meetingService)
                    .addTranscript(any(AddTranscriptCommand.class));

            assertThrows(RuntimeException.class, () -> handler.handle(event));
        }
    }

    // ============================================================
    // MEETING ENDED TESTS
    // ============================================================

    @Nested
    class MeetingEndedTests {

        @Test
        void shouldCallEndMeetingWithCorrectCommand() {
            UUID meetingId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            Instant endedAt = Instant.now();

            MeetingEndedEvent event = new MeetingEndedEvent(
                    meetingId,
                    sessionId,
                    "title",
                    "source",
                    Instant.now(),
                    Instant.now(),
                    endedAt,
                    UUID.randomUUID(),
                    "org",
                    "reason");

            handler.handle(event);

            ArgumentCaptor<EndMeetingCommand> captor = ArgumentCaptor.forClass(EndMeetingCommand.class);
            verify(meetingService).endMeeting(captor.capture());

            EndMeetingCommand cmd = captor.getValue();
            assertEquals(meetingId, cmd.getMeetingId());
            assertEquals(sessionId, cmd.getSessionId());
            assertEquals(endedAt, cmd.getEndedAt());
            assertEquals("reason", cmd.getReason());
        }

        @Test
        void shouldAllowNullReason() {
            MeetingEndedEvent event = new MeetingEndedEvent(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "title",
                    "source",
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    UUID.randomUUID(),
                    "org",
                    null);

            handler.handle(event);

            ArgumentCaptor<EndMeetingCommand> captor = ArgumentCaptor.forClass(EndMeetingCommand.class);
            verify(meetingService).endMeeting(captor.capture());
            assertNull(captor.getValue().getReason());
        }

        @Test
        void shouldPropagateExceptionFromService() {
            MeetingEndedEvent event = new MeetingEndedEvent(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "title",
                    "source",
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    UUID.randomUUID(),
                    "org",
                    "reason");

            doThrow(new RuntimeException("DB error"))
                    .when(meetingService)
                    .endMeeting(any(EndMeetingCommand.class));

            assertThrows(RuntimeException.class, () -> handler.handle(event));
        }
    }
}
