package com.github.meeting_platform.infrastructure.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.meeting_platform.domain.model.Transcript;
import com.github.meeting_platform.domain.service.MeetingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MeetingController {
    private final MeetingService meetingService;

    @GetMapping("/{id}/sessions/{sessionId}/transcript")
    public ResponseEntity<List<Transcript>> getSessionTranscript(@RequestParam("id") String meetingId,
            @RequestParam("sessionId") String sessionId) {
        log.info("Received request to get transcript for meetingId: {}, sessionId: {}", meetingId, sessionId);
        // fetch transcripts for the given meeting and session
        List<Transcript> transcripts = meetingService.getSessionTranscripts(UUID.fromString(meetingId),
                UUID.fromString(sessionId));
        return ResponseEntity.ok(transcripts);
    }
}
