package com.reduxr.controller;

import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    @GetMapping("/{appId}")
    public GameResponseDto getGameById(@PathVariable Long appId) {
        return gameService.getGameById(appId);
    }

    @GetMapping
    public Page<GameResponseDto> getGamesByParams(
            GameSearchParametersDto searchParametersDto) {
        return gameService.getGamesByParams(searchParametersDto);
    }
}
