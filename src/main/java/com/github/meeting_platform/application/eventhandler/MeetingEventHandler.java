package com.github.meeting_platform.application.eventhandler;

import org.springframework.stereotype.Component;

import com.github.meeting_platform.application.events.MeetingStartedEvent;
import com.github.meeting_platform.application.events.TranscriptAddedEvent;
import com.github.meeting_platform.application.events.MeetingEndedEvent;
import com.github.meeting_platform.domain.service.MeetingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingEventHandler {

    private final MeetingService meetingService;

    public void handle(MeetingStartedEvent event) {
        log.info("Handling MeetingStartedEvent: {}", event);
        meetingService.startMeeting(
                event.getId(),
                event.getSessionId(),
                event.getTitle(),
                event.getRoomName(),
                event.getOrganizedById(),
                event.getOrganizedByName(),
                event.getCreatedAt().toString(),
                event.getStartedAt().toString());
    }

    public void handle(TranscriptAddedEvent event) {
        log.info("Handling TranscriptAddedEvent: {}", event);
        meetingService.addTranscript(
                event.getMeetingId(),
                event.getSessionId(),
                event.getId(),
                event.getSequenceNumber(),
                event.getSpeakerId(),
                event.getSpeakerName(),
                event.getContent(),
                event.getStartOffset(),
                event.getEndOffset(),
                event.getLanguage());
    }

    public void handle(MeetingEndedEvent event) {
        log.info("Handling MeetingEndedEvent: {}", event);
        meetingService.endMeeting(
                event.getId(),
                event.getSessionId(),
                event.getEndedAt(),
                event.getReason());
    }
}
