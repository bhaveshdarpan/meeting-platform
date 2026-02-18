package com.github.meeting_platform.infrastructure.asyncevents;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringMeetingEventPublisher implements MeetingEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    // @Retryable(includes = DataAccessException.class, multiplier = 2.0)
    public void publish(Object event) {
        log.info("Publishing event: {}", event);
        applicationEventPublisher.publishEvent(event);
    }
}
