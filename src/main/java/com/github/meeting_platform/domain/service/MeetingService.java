package com.github.meeting_platform.domain.service;

import java.util.List;
import java.util.UUID;

import com.github.meeting_platform.domain.model.Transcript;
import com.github.meeting_platform.domain.service.command.AddTranscriptCommand;
import com.github.meeting_platform.domain.service.command.EndMeetingCommand;
import com.github.meeting_platform.domain.service.command.StartMeetingCommand;

public interface MeetingService {

    void startMeeting(StartMeetingCommand command);

    void addTranscript(AddTranscriptCommand command);

    void endMeeting(EndMeetingCommand command);

    List<Transcript> getSessionTranscripts(UUID meetingId, UUID sessionId);
}
