package com.reduxr.repository;

import com.reduxr.model.Game;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findGameById(Long id);
}
