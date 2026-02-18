package com.github.meeting_platform.common.exceptions;

public class SessionEndedException extends RuntimeException {
    public SessionEndedException(String message) {
        super(message);
    }
}
