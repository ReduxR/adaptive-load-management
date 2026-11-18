package com.reduxr.dto;

import java.time.Instant;

public record ErrorResponseDto(Instant timestamp, int status, String message) {
}
