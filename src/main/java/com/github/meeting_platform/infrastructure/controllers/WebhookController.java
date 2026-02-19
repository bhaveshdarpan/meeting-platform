package com.github.meeting_platform.infrastructure.controllers;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.github.meeting_platform.common.exceptions.InvalidEventException;

import com.github.meeting_platform.infrastructure.asyncevents.MeetingEventPublisher;
import com.github.meeting_platform.infrastructure.dto.MeetingStartedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingEndedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest;
import com.github.meeting_platform.infrastructure.validator.WebhookPayloadValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WebhookController {

    private final MeetingEventPublisher eventPublisher;
    private final WebhookPayloadValidator validator;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody JsonNode payload) {

        log.info("Processing webhook event: {}", payload);

        if (payload == null || !payload.has("event")) {
            throw new InvalidEventException("Missing required field: event");
        }

        String eventType = payload.get("event").asString();

        switch (eventType) {

            case "meeting.started":
                MeetingStartedWebhookRequest started = validator.convertAndValidate(payload,
                        MeetingStartedWebhookRequest.class);
                eventPublisher.publish(started);
                break;

            case "meeting.transcript":
                MeetingTranscriptWebhookRequest transcript = validator.convertAndValidate(payload,
                        MeetingTranscriptWebhookRequest.class);
                eventPublisher.publish(transcript);
                break;

            case "meeting.ended":
                MeetingEndedWebhookRequest ended = validator.convertAndValidate(payload,
                        MeetingEndedWebhookRequest.class);
                eventPublisher.publish(ended);
                break;

            default:
                throw new InvalidEventException("Unknown event type: " + eventType);
        }

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
