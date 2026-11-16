package com.reduxr.controller;

import com.reduxr.dto.CreateGameRequestDto;
import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.dto.UpdateGameRequestDto;
import com.reduxr.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponseDto createGame(@RequestBody CreateGameRequestDto requestDto) {
        return gameService.createGame(requestDto);
    }
    
    @PutMapping("/{gameId}")
    public GameResponseDto updateGame(
            @PathVariable Long gameId,
            @RequestBody UpdateGameRequestDto requestDto) {
        return gameService.updateGameById(gameId, requestDto);
    }
    
    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGame(@PathVariable Long gameId) {
        gameService.deleteGameById(gameId);
    }
}
