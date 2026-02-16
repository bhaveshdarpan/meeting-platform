package com.github.meeting_platform.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.github.meeting_platform.infrastructure.asyncevents.MeetingEventPublisher;
import com.github.meeting_platform.infrastructure.dto.MeetingEndedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingStartedWebhookRequest;
import com.github.meeting_platform.infrastructure.dto.MeetingTranscriptWebhookRequest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    MeetingEventPublisher eventPublisher;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    WebhookController controller;

    // ============================================================
    // WEBHOOK HANDLING TESTS
    // ============================================================

    @Nested
    class HandleWebhookTests {

        @Test
        void shouldPublishMeetingStartedEvent() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("meeting.started");

            MeetingStartedWebhookRequest dto = new MeetingStartedWebhookRequest();
            when(objectMapper.convertValue(payload, MeetingStartedWebhookRequest.class))
                    .thenReturn(dto);

            ResponseEntity<Map<String, String>> response = controller.handleWebhook(payload);

            assertEquals(202, response.getStatusCode().value());
            assertEquals("accepted", response.getBody().get("status"));

            verify(objectMapper)
                    .convertValue(payload, MeetingStartedWebhookRequest.class);

            verify(eventPublisher).publish(dto);
        }

        @Test
        void shouldPublishMeetingTranscriptEvent() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("meeting.transcript");

            MeetingTranscriptWebhookRequest dto = new MeetingTranscriptWebhookRequest();

            when(objectMapper.convertValue(payload,
                    MeetingTranscriptWebhookRequest.class))
                    .thenReturn(dto);

            ResponseEntity<Map<String, String>> response = controller.handleWebhook(payload);

            assertEquals(202, response.getStatusCode().value());
            verify(eventPublisher).publish(dto);
        }

        @Test
        void shouldPublishMeetingEndedEvent() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("meeting.ended");

            MeetingEndedWebhookRequest dto = new MeetingEndedWebhookRequest();

            when(objectMapper.convertValue(payload,
                    MeetingEndedWebhookRequest.class))
                    .thenReturn(dto);

            ResponseEntity<Map<String, String>> response = controller.handleWebhook(payload);

            assertEquals(202, response.getStatusCode().value());
            verify(eventPublisher).publish(dto);
        }

        @Test
        void shouldNotPublishForUnknownEvent() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("unknown.event");

            ResponseEntity<Map<String, String>> response = controller.handleWebhook(payload);

            assertEquals(202, response.getStatusCode().value());
            assertEquals("accepted", response.getBody().get("status"));

            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldThrowWhenEventMissing() {
            JsonNode payload = mock(JsonNode.class);
            when(payload.get("event")).thenReturn(null);

            assertThrows(NullPointerException.class,
                    () -> controller.handleWebhook(payload));

            verifyNoInteractions(eventPublisher);
        }

        @Test
        void shouldPropagateWhenObjectMapperFails() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("meeting.started");

            when(objectMapper.convertValue(any(),
                    eq(MeetingStartedWebhookRequest.class)))
                    .thenThrow(new RuntimeException("Mapping failed"));

            assertThrows(RuntimeException.class,
                    () -> controller.handleWebhook(payload));

            verify(eventPublisher, never()).publish(any());
        }

        @Test
        void shouldPropagateWhenPublisherFails() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("meeting.started");

            MeetingStartedWebhookRequest dto = new MeetingStartedWebhookRequest();

            when(objectMapper.convertValue(payload,
                    MeetingStartedWebhookRequest.class))
                    .thenReturn(dto);

            doThrow(new RuntimeException("Publish failed"))
                    .when(eventPublisher).publish(dto);

            assertThrows(RuntimeException.class,
                    () -> controller.handleWebhook(payload));
        }

        @Test
        void shouldAlwaysReturnAcceptedStatus() {
            JsonNode payload = mock(JsonNode.class);
            JsonNode eventNode = mock(JsonNode.class);

            when(payload.get("event")).thenReturn(eventNode);
            when(eventNode.asString()).thenReturn("meeting.started");

            when(objectMapper.convertValue(any(),
                    eq(MeetingStartedWebhookRequest.class)))
                    .thenReturn(new MeetingStartedWebhookRequest());

            ResponseEntity<Map<String, String>> response = controller.handleWebhook(payload);

            assertEquals(202, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("accepted", response.getBody().get("status"));
        }
    }

    // ============================================================
    // HEALTH ENDPOINT TESTS
    // ============================================================

    @Test
    void healthShouldReturnUpStatus() {
        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
    }
}
