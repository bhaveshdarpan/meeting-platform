package com.github.meeting_platform.infrastructure.asyncevents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.github.meeting_platform.infrastructure.dto.MeetingEndedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingStartedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest;
import com.github.meeting_platform.application.eventhandler.MeetingEventHandler;
import com.github.meeting_platform.domain.events.MeetingEndedEvent;
import com.github.meeting_platform.domain.events.MeetingStartedEvent;
import com.github.meeting_platform.domain.events.TranscriptAddedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingEventListener {

    private final MeetingEventHandler eventHandler;

    @EventListener
    public void on(MeetingStartedWebhookRequest request) {
        try {
            log.info("Received MeetingStartedWebhookRequest: meetingId={}, sessionId={}",
                    request.getMeeting() != null ? request.getMeeting().getId() : null,
                    request.getMeeting() != null ? request.getMeeting().getSessionId() : null);

            if (request.getMeeting() == null) {
                log.error("Invalid MeetingStartedWebhookRequest: meeting is null");
                return;
            }

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
        } catch (Exception e) {
            log.error("Error processing MeetingStartedWebhookRequest: meetingId={}, error={}",
                    request.getMeeting() != null ? request.getMeeting().getId() : null,
                    e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void on(MeetingTranscriptWebhookRequest request) {
        try {
            log.info("Received MeetingTranscriptWebhookRequest: transcriptId={}, meetingId={}, sessionId={}",
                    request.getData() != null ? request.getData().getTranscriptId() : null,
                    request.getMeeting() != null ? request.getMeeting().getId() : null,
                    request.getMeeting() != null ? request.getMeeting().getSessionId() : null);

            if (request.getMeeting() == null || request.getData() == null) {
                log.error("Invalid MeetingTranscriptWebhookRequest: meeting or data is null");
                return;
            }

            Duration startOffset = Duration.ofSeconds(request.getData().getStartOffset());
            Duration endOffset = Duration.ofSeconds(request.getData().getEndOffset());
            TranscriptAddedEvent event = new TranscriptAddedEvent(
                    request.getData().getTranscriptId(),
                    request.getMeeting().getId(),
                    request.getMeeting().getSessionId(),
                    request.getData().getSequenceNumber(),
                    request.getData().getSpeaker().getId(),
                    request.getData().getSpeaker().getName(),
                    request.getData().getContent(),
                    startOffset,
                    endOffset,
                    request.getData().getLanguage());

            eventHandler.handle(event);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input in MeetingTranscriptWebhookRequest: transcriptId={}, error={}",
                    request.getData() != null ? request.getData().getTranscriptId() : null,
                    e.getMessage(), e);
            // Don't rethrow - validation errors are non-retryable
        } catch (Exception e) {
            log.error("Error processing MeetingTranscriptWebhookRequest: transcriptId={}, error={}",
                    request.getData() != null ? request.getData().getTranscriptId() : null,
                    e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void on(MeetingEndedWebhookRequest request) {
        try {
            log.info("Received MeetingEndedWebhookRequest: meetingId={}, sessionId={}",
                    request.getMeeting() != null ? request.getMeeting().getId() : null,
                    request.getMeeting() != null ? request.getMeeting().getSessionId() : null);

            if (request.getMeeting() == null) {
                log.error("Invalid MeetingEndedWebhookRequest: meeting is null");
                return;
            }

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
        } catch (Exception e) {
            log.error("Error processing MeetingEndedWebhookRequest: meetingId={}, error={}",
                    request.getMeeting() != null ? request.getMeeting().getId() : null,
                    e.getMessage(), e);
        }
    }
}
