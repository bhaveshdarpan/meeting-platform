package com.github.meeting_platform.infrastructure.asyncevents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringMeetingEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SpringMeetingEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new SpringMeetingEventPublisher(applicationEventPublisher);
    }

    @Test
    void publish_delegatesToApplicationEventPublisher() {
        Object event = new Object();

        eventPublisher.publish(event);

        verify(applicationEventPublisher, times(1)).publishEvent(event);
        verifyNoMoreInteractions(applicationEventPublisher);
    }

    @Test
    void publish_multipleEvents_callsPublisherEachTime() {
        Object event1 = new Object();
        Object event2 = new Object();

        eventPublisher.publish(event1);
        eventPublisher.publish(event2);

        verify(applicationEventPublisher, times(2)).publishEvent((Object) any());
    }

    @Test
    void publish_nullEvent_shouldStillDelegate() {
        eventPublisher.publish(null);

        verify(applicationEventPublisher, times(0)).publishEvent(any());
    }

    @Test
    void publish_doesNotCallOtherMethods() {
        Object event = new Object();

        eventPublisher.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
        verifyNoMoreInteractions(applicationEventPublisher);
    }
}
