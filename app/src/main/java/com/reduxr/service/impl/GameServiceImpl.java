package com.reduxr.service.impl;

import com.reduxr.dto.CreateGameRequestDto;
import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.exception.EntityNotFoundException;
import com.reduxr.mapper.GameMapper;
import com.reduxr.model.Game;
import com.reduxr.repository.GameRepository;
import com.reduxr.search.GameSearchBuilder;
import com.reduxr.service.GameService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private final EntityManager entityManager;
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

    @Override
    @Transactional
    public GameResponseDto createGame(CreateGameRequestDto requestDto) {
        Game game = gameMapper.toModel(requestDto);
        entityManager.persist(game);
        entityManager.flush();
        return gameMapper.toDto(game);
    }
}
