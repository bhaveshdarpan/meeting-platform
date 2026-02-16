package com.github.meeting_platform.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.meeting_platform.application.events.MeetingEndedEvent;
import com.github.meeting_platform.application.events.MeetingStartedEvent;
import com.github.meeting_platform.application.events.TranscriptAddedEvent;
import com.github.meeting_platform.domain.service.MeetingService;

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
        void shouldCallStartMeetingWithCorrectArguments() {
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
                    started,
                    created,
                    organizer,
                    "org");

            handler.handle(event);

            verify(meetingService).startMeeting(
                    eq(meetingId),
                    eq(sessionId),
                    eq("title"),
                    eq("room"),
                    eq(organizer),
                    eq("org"),
                    anyString(),
                    anyString());
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

            verify(meetingService, times(2))
                    .startMeeting(eq(meetingId), eq(sessionId),
                            any(), any(), any(), any(),
                            anyString(), anyString());
        }

        @Test
        void shouldSwallowExceptionFromService() {
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
                    .startMeeting(any(), any(), any(), any(),
                            any(), any(), anyString(), anyString());

            assertDoesNotThrow(() -> handler.handle(event));
        }
    }

    // ============================================================
    // TRANSCRIPT ADDED TESTS
    // ============================================================

    @Nested
    class TranscriptAddedTests {

        @Test
        void shouldCallAddTranscriptWithCorrectArguments() {
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

            verify(meetingService).addTranscript(
                    eq(meetingId),
                    eq(sessionId),
                    eq(transcriptId),
                    eq(5),
                    eq(speakerId),
                    eq("Alice"),
                    eq("Hello world"),
                    eq(Duration.ofSeconds(1)),
                    eq(Duration.ofSeconds(3)),
                    eq("en"));
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

            verify(meetingService).addTranscript(
                    any(), any(), any(), anyInt(),
                    any(), isNull(),
                    any(), any(), any(), any());
        }

        @Test
        void shouldSwallowExceptionFromService() {
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
                    .addTranscript(any(), any(), any(),
                            anyInt(), any(), any(),
                            any(), any(), any(), any());

            assertDoesNotThrow(() -> handler.handle(event));
        }
    }

    // ============================================================
    // MEETING ENDED TESTS
    // ============================================================

    @Nested
    class MeetingEndedTests {

        @Test
        void shouldCallEndMeetingWithCorrectArguments() {
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

            verify(meetingService)
                    .endMeeting(
                            eq(meetingId),
                            eq(sessionId),
                            eq(endedAt),
                            eq("reason"));
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

            verify(meetingService)
                    .endMeeting(any(), any(), any(), isNull());
        }

        @Test
        void shouldSwallowExceptionFromService() {
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
                    .endMeeting(any(), any(), any(), any());

            assertDoesNotThrow(() -> handler.handle(event));
        }
    }

    // ============================================================
    // SAFETY TESTS
    // ============================================================

    // @Test
    // void shouldNotInteractWithServiceWhenNullEvent() {
    // assertDoesNotThrow(() -> handler.handle(null));
    // verifyNoInteractions(meetingService);
    // }
}
