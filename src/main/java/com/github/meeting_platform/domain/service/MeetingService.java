package com.github.meeting_platform.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.github.meeting_platform.domain.model.Transcript;

public interface MeetingService {

        void startMeeting(UUID meetingId, UUID sessionId, String title, String roomName, UUID organizedById,
                        String organizedByName, String createdAt, String startedAt);

        void addTranscript(
                        UUID meetingId,
                        UUID sessionId,
                        UUID transcriptId,
                        int sequenceNumber,
                        UUID speakerId,
                        String speakerName,
                        String content,
                        Duration startOffset,
                        Duration endOffset,
                        String language);

        void endMeeting(
                        UUID meetingId,
                        UUID sessionId,
                        Instant endedAt,
                        String reason);

        List<Transcript> getSessionTranscripts(UUID meetingId, UUID sessionId);
}
