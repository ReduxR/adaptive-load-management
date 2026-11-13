package com.reduxr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GameResponseDto(
        Long id,
        String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
        LocalDate releaseDate,
        BigDecimal price,
        String shortDescription) {
}
