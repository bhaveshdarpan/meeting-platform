package com.github.meeting_platform.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.github.meeting_platform.domain.model.Transcript;
import com.github.meeting_platform.domain.service.MeetingService;

@ExtendWith(MockitoExtension.class)
class MeetingControllerTest {

    @Mock
    private MeetingService meetingService;

    @InjectMocks
    private MeetingController controller;

    private UUID meetingId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        meetingId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    void getSessionTranscript_returnsEmptyList() {
        when(meetingService.getSessionTranscripts(meetingId, sessionId))
                .thenReturn(List.of());

        ResponseEntity<List<Transcript>> response = controller.getSessionTranscript(meetingId.toString(),
                sessionId.toString());

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(meetingService, times(1))
                .getSessionTranscripts(meetingId, sessionId);
        verifyNoMoreInteractions(meetingService);
    }

    @Test
    void getSessionTranscript_returnsSingleTranscript() {
        Transcript transcript = new Transcript();
        when(meetingService.getSessionTranscripts(meetingId, sessionId))
                .thenReturn(List.of(transcript));

        ResponseEntity<List<Transcript>> response = controller.getSessionTranscript(meetingId.toString(),
                sessionId.toString());

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(transcript, response.getBody().get(0));

        verify(meetingService).getSessionTranscripts(meetingId, sessionId);
    }

    @Test
    void getSessionTranscript_returnsMultipleTranscripts() {
        Transcript t1 = new Transcript();
        Transcript t2 = new Transcript();

        when(meetingService.getSessionTranscripts(meetingId, sessionId))
                .thenReturn(List.of(t1, t2));

        ResponseEntity<List<Transcript>> response = controller.getSessionTranscript(meetingId.toString(),
                sessionId.toString());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().containsAll(List.of(t1, t2)));

        verify(meetingService).getSessionTranscripts(meetingId, sessionId);
    }

    @Test
    void getSessionTranscript_invalidMeetingUuid_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getSessionTranscript("invalid-uuid", sessionId.toString()));

        verifyNoInteractions(meetingService);
    }

    @Test
    void getSessionTranscript_invalidSessionUuid_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getSessionTranscript(meetingId.toString(), "invalid-uuid"));

        verifyNoInteractions(meetingService);
    }

    @Test
    void getSessionTranscript_nullMeetingId_throwsException() {
        assertThrows(NullPointerException.class,
                () -> controller.getSessionTranscript(null, sessionId.toString()));

        verifyNoInteractions(meetingService);
    }

    @Test
    void getSessionTranscript_nullSessionId_throwsException() {
        assertThrows(NullPointerException.class,
                () -> controller.getSessionTranscript(meetingId.toString(), null));

        verifyNoInteractions(meetingService);
    }

    @Test
    void getSessionTranscript_serviceThrows_propagatesException() {
        when(meetingService.getSessionTranscripts(meetingId, sessionId))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(RuntimeException.class,
                () -> controller.getSessionTranscript(meetingId.toString(), sessionId.toString()));

        verify(meetingService).getSessionTranscripts(meetingId, sessionId);
    }
}
