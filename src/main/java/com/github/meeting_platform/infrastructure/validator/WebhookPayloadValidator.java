package com.github.meeting_platform.infrastructure.validator;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.meeting_platform.common.exceptions.InvalidEventException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class WebhookPayloadValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public WebhookPayloadValidator(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> T convertAndValidate(JsonNode payload, Class<T> clazz) {
        try {
            T request = objectMapper.convertValue(payload, clazz);

            Set<ConstraintViolation<T>> violations = validator.validate(request);

            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new InvalidEventException("Validation failed: " + errorMessage);
            }

            return request;

        } catch (IllegalArgumentException e) {
            throw new InvalidEventException("Invalid JSON structure: " + e.getMessage());
        }
    }
}
