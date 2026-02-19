package com.github.meeting_platform.infrastructure.asyncevents;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.meeting_platform.application.eventhandler.MeetingEventHandler;
import com.github.meeting_platform.domain.events.MeetingEndedEvent;
import com.github.meeting_platform.domain.events.MeetingStartedEvent;
import com.github.meeting_platform.domain.events.TranscriptAddedEvent;
import com.github.meeting_platform.infrastructure.dto.*;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest.TranscriptData;

@ExtendWith(MockitoExtension.class)
class MeetingEventListenerTest {

    @Mock
    private MeetingEventHandler eventHandler;

    @InjectMocks
    private MeetingEventListener listener;

    @Test
    void on_MeetingStartedWebhookRequest_createsAndHandlesEvent() {
        // Arrange
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        MeetingStartedWebhookRequest request = mock(MeetingStartedWebhookRequest.class);
        MeetingStartedWebhookRequest.Meeting meeting = mock(MeetingStartedWebhookRequest.Meeting.class);
        MeetingStartedWebhookRequest.OrganizedBy organizer = mock(MeetingStartedWebhookRequest.OrganizedBy.class);

        when(request.getMeeting()).thenReturn(meeting);
        when(meeting.getId()).thenReturn(meetingId);
        when(meeting.getSessionId()).thenReturn(sessionId);
        when(meeting.getTitle()).thenReturn("Title");
        when(meeting.getRoomName()).thenReturn("Room");
        when(meeting.getStatus()).thenReturn("STARTED");
        when(meeting.getCreatedAt()).thenReturn(Instant.now());
        when(meeting.getStartedAt()).thenReturn(Instant.now());
        when(meeting.getOrganizedBy()).thenReturn(organizer);
        when(organizer.getId()).thenReturn(organizerId);
        when(organizer.getName()).thenReturn("John");

        // Act
        listener.on(request);

        // Assert
        ArgumentCaptor<MeetingStartedEvent> captor = ArgumentCaptor.forClass(MeetingStartedEvent.class);

        verify(eventHandler).handle(captor.capture());
        verifyNoMoreInteractions(eventHandler);

        MeetingStartedEvent event = captor.getValue();
        assert event.getId().equals(meetingId);
        assert event.getSessionId().equals(sessionId);
    }

    @Test
    void on_MeetingTranscriptWebhookRequest_createsAndHandlesEvent() {
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID transcriptId = UUID.randomUUID();
        UUID speakerId = UUID.randomUUID();

        MeetingTranscriptWebhookRequest request = mock(MeetingTranscriptWebhookRequest.class);
        MeetingTranscriptWebhookRequest.Meeting meeting = mock(MeetingTranscriptWebhookRequest.Meeting.class);
        TranscriptData data = mock(TranscriptData.class);
        MeetingTranscriptWebhookRequest.Speaker speaker = mock(MeetingTranscriptWebhookRequest.Speaker.class);

        when(request.getMeeting()).thenReturn(meeting);
        when(request.getData()).thenReturn(data);

        when(meeting.getId()).thenReturn(meetingId);
        when(meeting.getSessionId()).thenReturn(sessionId);

        when(data.getTranscriptId()).thenReturn(transcriptId);
        when(data.getSequenceNumber()).thenReturn(1);
        when(data.getContent()).thenReturn("Hello world");
        // when(data.getStartOffset()).thenReturn(0L);
        // when(data.getEndOffset()).thenReturn(10L);
        when(data.getLanguage()).thenReturn("en");
        when(data.getSpeaker()).thenReturn(speaker);

        when(speaker.getId()).thenReturn(speakerId);
        when(speaker.getName()).thenReturn("Alice");

        listener.on(request);

        ArgumentCaptor<TranscriptAddedEvent> captor = ArgumentCaptor.forClass(TranscriptAddedEvent.class);

        verify(eventHandler).handle(captor.capture());
        verifyNoMoreInteractions(eventHandler);

        TranscriptAddedEvent event = captor.getValue();
        assert event.getMeetingId().equals(meetingId);
        assert event.getId().equals(transcriptId);
    }

    @Test
    void on_MeetingEndedWebhookRequest_createsAndHandlesEvent() {
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        MeetingEndedWebhookRequest request = mock(MeetingEndedWebhookRequest.class);
        MeetingEndedWebhookRequest.Meeting meeting = mock(MeetingEndedWebhookRequest.Meeting.class);
        MeetingEndedWebhookRequest.OrganizedBy organizer = mock(MeetingEndedWebhookRequest.OrganizedBy.class);

        when(request.getMeeting()).thenReturn(meeting);
        when(request.getReason()).thenReturn("FINISHED");

        when(meeting.getId()).thenReturn(meetingId);
        when(meeting.getSessionId()).thenReturn(sessionId);
        when(meeting.getTitle()).thenReturn("Title");
        when(meeting.getStatus()).thenReturn("ENDED");
        when(meeting.getCreatedAt()).thenReturn(Instant.now());
        when(meeting.getStartedAt()).thenReturn(Instant.now());
        when(meeting.getEndedAt()).thenReturn(Instant.now());
        when(meeting.getOrganizedBy()).thenReturn(organizer);

        when(organizer.getId()).thenReturn(organizerId);
        when(organizer.getName()).thenReturn("John");

        listener.on(request);

        ArgumentCaptor<MeetingEndedEvent> captor = ArgumentCaptor.forClass(MeetingEndedEvent.class);

        verify(eventHandler).handle(captor.capture());
        verifyNoMoreInteractions(eventHandler);

        MeetingEndedEvent event = captor.getValue();
        assert event.getId().equals(meetingId);
        assert event.getSessionId().equals(sessionId);
    }
}
