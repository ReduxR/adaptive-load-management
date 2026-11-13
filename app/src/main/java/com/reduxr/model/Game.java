package com.reduxr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "games")
public class Game {
    @Id
    @Column(name = "app_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDate releaseDate;

    private BigDecimal price;

    private String shortDescription;
}
