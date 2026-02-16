package com.github.meeting_platform.infrastructure.asyncevents;

public interface MeetingEventPublisher {
    void publish(Object event);
}
