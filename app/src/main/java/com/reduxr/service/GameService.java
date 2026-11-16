package com.reduxr.service;

import com.reduxr.dto.CreateGameRequestDto;
import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.dto.UpdateGameRequestDto;
import org.springframework.data.domain.Page;

public interface GameService {
    GameResponseDto getGameById(Long gameId);

    Page<GameResponseDto> getGamesByParams(GameSearchParametersDto parametersDto);

    GameResponseDto createGame(CreateGameRequestDto requestDto);
    
    GameResponseDto updateGameById(Long gameId, UpdateGameRequestDto requestDto);
}
