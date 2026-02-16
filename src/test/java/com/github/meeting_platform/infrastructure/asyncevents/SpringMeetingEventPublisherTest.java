package com.github.meeting_platform.infrastructure.asyncevents;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringMeetingEventPublisherTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private SpringMeetingEventPublisher eventPublisher;

    @Test
    void publish_delegatesToApplicationEventPublisher() {
        Object event = new Object();

        eventPublisher.publish(event);

        verify(publisher, times(1)).publishEvent(event);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    void publish_multipleEvents_callsPublisherEachTime() {
        Object event1 = new Object();
        Object event2 = new Object();

        eventPublisher.publish(event1);
        eventPublisher.publish(event2);

        verify(publisher).publishEvent(event1);
        verify(publisher).publishEvent(event2);
        verify(publisher, times(2)).publishEvent(any());
    }

    @Test
    void publish_nullEvent_shouldStillDelegate() {
        eventPublisher.publish(null);

        verify(publisher).publishEvent(null);
    }

    @Test
    void publish_doesNotCallOtherMethods() {
        Object event = new Object();

        eventPublisher.publish(event);

        verify(publisher).publishEvent(event);
        verifyNoMoreInteractions(publisher);
    }
}
