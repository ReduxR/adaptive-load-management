package com.reduxr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameSearchParametersDto {
    private Integer page;
    private Integer size;
    private String name;
    private LocalDate releaseDate;
    private BigDecimal price;
}
