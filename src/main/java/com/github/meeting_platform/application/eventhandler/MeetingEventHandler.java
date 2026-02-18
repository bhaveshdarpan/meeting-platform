package com.github.meeting_platform.application.eventhandler;

import org.springframework.stereotype.Component;

import com.github.meeting_platform.application.events.MeetingEndedEvent;
import com.github.meeting_platform.application.events.MeetingStartedEvent;
import com.github.meeting_platform.application.events.TranscriptAddedEvent;
import com.github.meeting_platform.domain.service.MeetingService;
import com.github.meeting_platform.domain.service.command.AddTranscriptCommand;
import com.github.meeting_platform.domain.service.command.EndMeetingCommand;
import com.github.meeting_platform.domain.service.command.StartMeetingCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles application events and delegates to the domain service.
 * Uses template method for consistent error handling (DRY, Single
 * Responsibility).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingEventHandler {

        private final MeetingService meetingService;

        public void handle(MeetingStartedEvent event) {
                executeWithLogging(
                                "MeetingStartedEvent",
                                () -> String.format("meetingId=%s, sessionId=%s", event.getId(), event.getSessionId()),
                                () -> meetingService.startMeeting(new StartMeetingCommand(
                                                event.getId(),
                                                event.getSessionId(),
                                                event.getTitle(),
                                                event.getRoomName(),
                                                event.getOrganizedById(),
                                                event.getOrganizedByName(),
                                                event.getCreatedAt(),
                                                event.getStartedAt())));
        }

        public void handle(TranscriptAddedEvent event) {
                executeWithLogging(
                                "TranscriptAddedEvent",
                                () -> String.format("transcriptId=%s, meetingId=%s, sessionId=%s, sequenceNumber=%s",
                                                event.getId(), event.getMeetingId(), event.getSessionId(),
                                                event.getSequenceNumber()),
                                () -> meetingService.addTranscript(new AddTranscriptCommand(
                                                event.getMeetingId(),
                                                event.getSessionId(),
                                                event.getId(),
                                                event.getSequenceNumber(),
                                                event.getSpeakerId(),
                                                event.getSpeakerName(),
                                                event.getContent(),
                                                event.getStartOffset(),
                                                event.getEndOffset(),
                                                event.getLanguage())));
        }

        public void handle(MeetingEndedEvent event) {
                executeWithLogging(
                                "MeetingEndedEvent",
                                () -> String.format("meetingId=%s, sessionId=%s", event.getId(), event.getSessionId()),
                                () -> meetingService.endMeeting(new EndMeetingCommand(
                                                event.getId(),
                                                event.getSessionId(),
                                                event.getEndedAt(),
                                                event.getReason())));
        }

        /**
         * Template method for consistent error handling and logging.
         * Non-retryable exceptions (IllegalArgumentException, domain exceptions) are
         * rethrown.
         */
        private void executeWithLogging(String eventName, java.util.function.Supplier<String> contextSupplier,
                        Runnable action) {
                String context = contextSupplier.get();
                try {
                        log.info("Handling {}: {}", eventName, context);
                        action.run();
                        log.debug("Successfully processed {}: {}", eventName, context);
                } catch (IllegalArgumentException e) {
                        log.error("Non-retryable error processing {}: {}, error={}", eventName, context, e.getMessage(),
                                        e);
                        throw e;
                } catch (Exception e) {
                        log.error("Error processing {}: {}, error={}", eventName, context, e.getMessage(), e);
                        throw e;
                }
        }
}
