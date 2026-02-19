package com.github.meeting_platform;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.github.meeting_platform.domain.eventhandler.MeetingEventHandler;
import com.github.meeting_platform.domain.service.MeetingService;
import com.github.meeting_platform.infrastructure.controllers.MeetingController;

@SpringBootTest
class MeetingPlatformApplicationTests {

	@Test
	void contextLoadsSuccessfully() {
		// If the context fails to start, this test fails automatically
		assertTrue(true);
	}

	@Autowired
	private ApplicationContext context;

	@Test
	void shouldContainMeetingServiceBean() {
		assertNotNull(context.getBean(MeetingService.class));
	}

	@Test
	void shouldContainMeetingControllerBean() {
		assertNotNull(context.getBean(MeetingController.class));
	}

	@Test
	void shouldContainMeetingEventHandlerBean() {
		assertNotNull(context.getBean(MeetingEventHandler.class));
	}

}
