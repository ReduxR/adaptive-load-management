package com.reduxr.service.impl;

import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.exception.EntityNotFoundException;
import com.reduxr.mapper.GameMapper;
import com.reduxr.model.Game;
import com.reduxr.repository.GameRepository;
import com.reduxr.search.GameSearchBuilder;
import com.reduxr.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final GameMapper gameMapper;
    private final GameSearchBuilder gameSearchBuilder;

    @Override
    public GameResponseDto getGameById(Long gameId) {
        Game game = gameRepository.findGameById(gameId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Game with id " + gameId + " not found")
                );
        return gameMapper.toDto(game);
    }

    @Override
    public Page<GameResponseDto> getGamesByParams(GameSearchParametersDto parametersDto) {
        return gameSearchBuilder.searchGamesByParams(parametersDto)
                .map(gameMapper::toDto);
    }
}
