package com.github.meeting_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MeetingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeetingPlatformApplication.class, args);
	}

}
