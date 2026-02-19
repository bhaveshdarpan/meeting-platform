package com.github.meeting_platform.infrastructure.controllers;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.github.meeting_platform.common.exceptions.InvalidEventException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.github.meeting_platform.infrastructure.asyncevents.MeetingEventPublisher;
import com.github.meeting_platform.infrastructure.dto.MeetingStartedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingEndedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WebhookController {

    private final MeetingEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody JsonNode payload)
            throws MethodArgumentNotValidException {
        log.info("Processing webhook event: {}", payload);

        // Validate event field exists
        if (payload == null || !payload.has("event")) {
            throw new InvalidEventException("Missing required field: event");
        }

        String eventType = payload.get("event").asString();
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new InvalidEventException("Event type cannot be null or empty");
        }

        try {
            switch (eventType) {
                case "meeting.started":
                    log.info("Received meeting.started event: {}", payload);
                    MeetingStartedWebhookRequest startedRequest = objectMapper.convertValue(payload,
                            MeetingStartedWebhookRequest.class);
                    Set<ConstraintViolation<MeetingStartedWebhookRequest>> startedViolations = validator
                            .validate(startedRequest);
                    if (!startedViolations.isEmpty()) {
                        String errorMessage = startedViolations.stream()
                                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                                .collect(Collectors.joining(", "));
                        throw new InvalidEventException("Validation failed: " + errorMessage);
                    }
                    eventPublisher.publish(startedRequest);
                    break;

                case "meeting.transcript":
                    log.info("Received meeting.transcript event: {}", payload);
                    MeetingTranscriptWebhookRequest transcriptRequest = objectMapper.convertValue(payload,
                            MeetingTranscriptWebhookRequest.class);
                    Set<ConstraintViolation<MeetingTranscriptWebhookRequest>> violations = validator
                            .validate(transcriptRequest);
                    if (!violations.isEmpty()) {
                        String errorMessage = violations.stream()
                                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                                .collect(Collectors.joining(", "));
                        throw new InvalidEventException("Validation failed: " + errorMessage);
                    }

                    eventPublisher.publish(transcriptRequest);
                    break;

                case "meeting.ended":
                    log.info("Received meeting.ended event: {}", payload);
                    MeetingEndedWebhookRequest endedRequest = objectMapper.convertValue(payload,
                            MeetingEndedWebhookRequest.class);
                    Set<ConstraintViolation<MeetingEndedWebhookRequest>> endedViolations = validator
                            .validate(endedRequest);
                    if (!endedViolations.isEmpty()) {
                        String errorMessage = endedViolations.stream()
                                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                                .collect(Collectors.joining(", "));
                        throw new InvalidEventException("Validation failed: " + errorMessage);
                    }
                    eventPublisher.publish(endedRequest);
                    break;

                default:
                    log.warn("Received unknown event type: {}", eventType);
                    throw new InvalidEventException("Unknown event type: " + eventType);
            }
        } catch (MismatchedInputException e) {
            log.error("JSON parsing error for event type {}: {}", eventType, e.getMessage());
            throw new InvalidEventException("Invalid JSON structure: " + e.getOriginalMessage());
        } catch (Exception e) {
            log.error("Error processing event type {}: {}", eventType, e.getMessage());
            throw new InvalidEventException("Error processing event: " + e.getMessage());
        }

        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
