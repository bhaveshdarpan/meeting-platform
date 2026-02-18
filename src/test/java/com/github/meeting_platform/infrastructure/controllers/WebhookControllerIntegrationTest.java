package com.github.meeting_platform.infrastructure.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.meeting_platform.domain.service.command.AddTranscriptCommand;
import com.github.meeting_platform.domain.service.command.StartMeetingCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.meeting_platform.domain.model.Transcript;
import com.github.meeting_platform.domain.service.MeetingService;

/**
 * Integration tests for webhook controller end-to-end scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeetingService meetingService;

    @Test
    void testHappyPathMeetingLifecycle() throws Exception {
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        // 1. Send meeting.started
        String startedPayload = """
                {
                  "event": "meeting.started",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s",
                    "title": "Test Meeting",
                    "roomName": "test-room",
                    "status": "LIVE",
                    "createdAt": "2024-12-13T06:57:09.736Z",
                    "startedAt": "2024-12-13T06:57:09.736Z",
                    "organizedBy": {
                      "id": "%s",
                      "name": "Test Organizer"
                    }
                  }
                }
                """.formatted(meetingId, sessionId, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(startedPayload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Wait for async processing (meeting.started is synchronous, but give time for DB)
        Thread.sleep(800);

        // 2. Send transcript chunks
        UUID transcriptId1 = UUID.randomUUID();
        String transcript1 = """
                {
                  "event": "meeting.transcript",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s"
                  },
                  "data": {
                    "transcriptId": "%s",
                    "sequenceNumber": 1,
                    "speaker": {
                      "id": "%s",
                      "name": "Speaker 1"
                    },
                    "content": "First message",
                    "startOffset": 2,
                    "endOffset": 5,
                    "language": "en"
                  }
                }
                """.formatted(meetingId, sessionId, transcriptId1, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transcript1))
                .andExpect(status().isAccepted());

        Thread.sleep(1000);

        UUID transcriptId2 = UUID.randomUUID();
        String transcript2 = """
                {
                  "event": "meeting.transcript",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s"
                  },
                  "data": {
                    "transcriptId": "%s",
                    "sequenceNumber": 2,
                    "speaker": {
                      "id": "%s",
                      "name": "Speaker 1"
                    },
                    "content": "Second message",
                    "startOffset": 5,
                    "endOffset": 8,
                    "language": "en"
                  }
                }
                """.formatted(meetingId, sessionId, transcriptId2, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transcript2))
                .andExpect(status().isAccepted());

        Thread.sleep(1000);

        // 3. Send meeting.ended
        String endedPayload = """
                {
                  "event": "meeting.ended",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s",
                    "title": "Test Meeting",
                    "status": "LIVE",
                    "createdAt": "2024-12-13T06:57:09.736Z",
                    "startedAt": "2024-12-13T06:57:09.736Z",
                    "endedAt": "2024-12-13T07:04:37.052Z",
                    "organizedBy": {
                      "id": "%s",
                      "name": "Test Organizer"
                    }
                  },
                  "reason": "HOST_ENDED_MEETING"
                }
                """.formatted(meetingId, sessionId, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(endedPayload))
                .andExpect(status().isAccepted());

        // 4. Verify transcript retrieval (async processing - poll until ready)
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<Transcript> transcripts = meetingService.getSessionTranscripts(meetingId, sessionId);
            assertThat(transcripts).hasSize(2);
        });
        var transcripts = meetingService.getSessionTranscripts(meetingId, sessionId);
        assertThat(transcripts.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(transcripts.get(0).getContent()).isEqualTo("First message");
        assertThat(transcripts.get(1).getSequenceNumber()).isEqualTo(2);
        assertThat(transcripts.get(1).getContent()).isEqualTo("Second message");
    }

    @Test
    void testIdempotentDuplicateTranscript() throws Exception {
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID transcriptId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        // Start meeting
        String startedPayload = """
                {
                  "event": "meeting.started",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s",
                    "title": "Test Meeting",
                    "roomName": "test-room",
                    "status": "LIVE",
                    "createdAt": "2024-12-13T06:57:09.736Z",
                    "startedAt": "2024-12-13T06:57:09.736Z",
                    "organizedBy": {
                      "id": "%s",
                      "name": "Test Organizer"
                    }
                  }
                }
                """.formatted(meetingId, sessionId, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(startedPayload))
                .andExpect(status().isAccepted());

        Thread.sleep(800);

        // Send transcript first time
        String transcriptPayload = """
                {
                  "event": "meeting.transcript",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s"
                  },
                  "data": {
                    "transcriptId": "%s",
                    "sequenceNumber": 1,
                    "speaker": {
                      "id": "%s",
                      "name": "Speaker 1"
                    },
                    "content": "First message",
                    "startOffset": 2,
                    "endOffset": 5,
                    "language": "en"
                  }
                }
                """.formatted(meetingId, sessionId, transcriptId, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transcriptPayload))
                .andExpect(status().isAccepted());

        Thread.sleep(1000);

        // Send duplicate transcript (same transcriptId)
        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transcriptPayload))
                .andExpect(status().isAccepted()); // Should be accepted

        // Verify only one transcript exists (idempotent, async - poll until ready)
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<Transcript> t = meetingService.getSessionTranscripts(meetingId, sessionId);
            assertThat(t).hasSize(1);
        });
        var transcripts = meetingService.getSessionTranscripts(meetingId, sessionId);
        assertThat(transcripts).hasSize(1);
        assertThat(transcripts.get(0).getId()).isEqualTo(transcriptId);
    }

    @Test
    void testTranscriptAfterSessionEnded() throws Exception {
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        // Start and end meeting
        String startedPayload = """
                {
                  "event": "meeting.started",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s",
                    "title": "Test Meeting",
                    "roomName": "test-room",
                    "status": "LIVE",
                    "createdAt": "2024-12-13T06:57:09.736Z",
                    "startedAt": "2024-12-13T06:57:09.736Z",
                    "organizedBy": {
                      "id": "%s",
                      "name": "Test Organizer"
                    }
                  }
                }
                """.formatted(meetingId, sessionId, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(startedPayload))
                .andExpect(status().isAccepted());

        Thread.sleep(800);

        String endedPayload = """
                {
                  "event": "meeting.ended",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s",
                    "title": "Test Meeting",
                    "status": "LIVE",
                    "createdAt": "2024-12-13T06:57:09.736Z",
                    "startedAt": "2024-12-13T06:57:09.736Z",
                    "endedAt": "2024-12-13T07:04:37.052Z",
                    "organizedBy": {
                      "id": "%s",
                      "name": "Test Organizer"
                    }
                  },
                  "reason": "HOST_ENDED_MEETING"
                }
                """.formatted(meetingId, sessionId, organizerId);

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(endedPayload))
                .andExpect(status().isAccepted());

        Thread.sleep(1000);

        // Try to add transcript after session ended
        UUID transcriptId = UUID.randomUUID();
        String transcriptPayload = """
                {
                  "event": "meeting.transcript",
                  "meeting": {
                    "id": "%s",
                    "sessionId": "%s"
                  },
                  "data": {
                    "transcriptId": "%s",
                    "sequenceNumber": 1,
                    "speaker": {
                      "id": "%s",
                      "name": "Speaker 1"
                    },
                    "content": "Late message",
                    "startOffset": 2,
                    "endOffset": 5,
                    "language": "en"
                  }
                }
                """.formatted(meetingId, sessionId, transcriptId, organizerId);

        // Webhook is accepted; transcript is saved even after session ended (late delivery)
        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transcriptPayload))
                .andExpect(status().isAccepted());

        // Verify transcript was saved (late delivery, async - poll until ready)
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            List<Transcript> t = meetingService.getSessionTranscripts(meetingId, sessionId);
            assertThat(t).hasSize(1);
        });
        var transcripts = meetingService.getSessionTranscripts(meetingId, sessionId);
        assertThat(transcripts).hasSize(1);
        assertThat(transcripts.get(0).getContent()).isEqualTo("Late message");
    }

    @Test
    void testInvalidEventType() throws Exception {
        String invalidPayload = """
                {
                  "event": "unknown.event",
                  "meeting": {
                    "id": "50c8940e-1b97-402a-97d6-2708b7feca41"
                  }
                }
                """;

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingRequiredFields() throws Exception {
        String invalidPayload = """
                {
                  "event": "meeting.transcript",
                  "meeting": {
                    "id": "50c8940e-1b97-402a-97d6-2708b7feca41"
                  }
                }
                """;

        mockMvc.perform(post("/api/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetTranscriptEndpoint() throws Exception {
        UUID meetingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();

        // Create meeting and session manually for this test
        Instant instant = Instant.parse("2024-12-13T06:57:09.736Z");
        meetingService.startMeeting(new StartMeetingCommand(
                meetingId, sessionId, "Test", "Room",
                organizerId, "Organizer", instant, instant));

        UUID transcriptId = UUID.randomUUID();
        meetingService.addTranscript(new AddTranscriptCommand(
                meetingId, sessionId, transcriptId, 1,
                organizerId, "Speaker", "Test content",
                Duration.ofSeconds(2), Duration.ofSeconds(5), "en"));

        mockMvc.perform(get("/api/meetings/{meetingId}/sessions/{sessionId}/transcript",
                meetingId, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Test content"));
    }
}
