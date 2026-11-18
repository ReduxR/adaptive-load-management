package com.reduxr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.reduxr.validation.ValidReleaseDate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateGameRequestDto {
    @NotBlank
    private String name;

    @ValidReleaseDate(minYear = 1980)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    private LocalDate releaseDate;

    @Min(0)
    @NotNull
    private BigDecimal price;

    @NotBlank
    private String shortDescription;
}
