package com.github.meeting_platform.infrastructure.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.github.meeting_platform.infrastructure.asyncevents.MeetingEventPublisher;
import com.github.meeting_platform.infrastructure.dto.MeetingStartedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingEndedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WebhookController {

    private final MeetingEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody JsonNode payload) {
        log.info("Processing webhook event: {}", payload);

        String eventType = payload.get("event").asString();

        switch (eventType) {

            case "meeting.started":
                log.info("Received meeting.started event: {}", payload);
                MeetingStartedWebhookRequest startedRequest = objectMapper.convertValue(payload,
                        MeetingStartedWebhookRequest.class);
                eventPublisher.publish(startedRequest);
                break;

            case "meeting.transcript":
                log.info("Received meeting.transcript event: {}", payload);
                MeetingTranscriptWebhookRequest transcriptRequest = objectMapper.convertValue(payload,
                        MeetingTranscriptWebhookRequest.class);
                eventPublisher.publish(transcriptRequest);
                break;

            case "meeting.ended":
                log.info("Received meeting.ended event: {}", payload);
                MeetingEndedWebhookRequest endedRequest = objectMapper.convertValue(payload,
                        MeetingEndedWebhookRequest.class);
                eventPublisher.publish(endedRequest);
                break;

            default:
                log.warn("Received unknown event type: {}", eventType);
                break;
        }

        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
