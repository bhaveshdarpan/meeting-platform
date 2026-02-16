package com.github.meeting_platform.infrastructure.asyncevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.meeting_platform.application.events.MeetingStartedEvent;
import com.github.meeting_platform.application.events.TranscriptAddedEvent;
import com.github.meeting_platform.infrastructure.dto.MeetingEndedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingStartedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest;
import com.github.meeting_platform.application.events.MeetingEndedEvent;
import com.github.meeting_platform.application.eventhandler.MeetingEventHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingEventListener {

    private final MeetingEventHandler eventHandler;

    @Async
    @EventListener
    public void on(MeetingStartedWebhookRequest request) {
        log.info("Received MeetingStartedWebhookRequest: {}", request);
        MeetingStartedEvent event = new MeetingStartedEvent(
                request.getMeeting().getId(),
                request.getMeeting().getSessionId(),
                request.getMeeting().getTitle(),
                request.getMeeting().getRoomName(),
                request.getMeeting().getStatus(),
                request.getMeeting().getCreatedAt(),
                request.getMeeting().getStartedAt(),
                request.getMeeting().getOrganizedBy().getId(),
                request.getMeeting().getOrganizedBy().getName());
        eventHandler.handle(event);
    }

    @Async
    @EventListener
    public void on(MeetingTranscriptWebhookRequest request) {
        log.info("Received MeetingTranscriptWebhookRequest: {}", request);
        TranscriptAddedEvent event = new TranscriptAddedEvent(
                request.getData().getTranscriptId(),
                request.getMeeting().getId(),
                request.getMeeting().getSessionId(),
                request.getData().getSequenceNumber(),
                request.getData().getSpeaker().getId(),
                request.getData().getSpeaker().getName(),
                request.getData().getContent(),
                request.getData().getStartOffset(),
                request.getData().getEndOffset(),
                request.getData().getLanguage());

        eventHandler.handle(event);
    }

    @Async
    @EventListener
    public void on(MeetingEndedWebhookRequest request) {
        log.info("Received MeetingEndedWebhookRequest: {}", request);
        MeetingEndedEvent event = new MeetingEndedEvent(
                request.getMeeting().getId(),
                request.getMeeting().getSessionId(),
                request.getMeeting().getTitle(),
                // request.getMeeting().getRoomName(),
                request.getMeeting().getStatus(),
                request.getMeeting().getCreatedAt(),
                request.getMeeting().getStartedAt(),
                request.getMeeting().getEndedAt(),
                request.getMeeting().getOrganizedBy().getId(),
                request.getMeeting().getOrganizedBy().getName(),
                request.getReason());
        eventHandler.handle(event);
    }
}
