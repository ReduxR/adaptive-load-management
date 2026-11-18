package com.reduxr.service.impl;

import com.reduxr.dto.CreateGameRequestDto;
import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.dto.UpdateGameRequestDto;
import com.reduxr.exception.EntityNotFoundException;
import com.reduxr.exception.GameAlreadyExistsException;
import com.reduxr.mapper.GameMapper;
import com.reduxr.model.Game;
import com.reduxr.repository.GameRepository;
import com.reduxr.search.GameSearchBuilder;
import com.reduxr.service.GameService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
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
        Game game = getGameOrThrow(gameId);
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
        try {
            entityManager.persist(game);
            entityManager.flush();
        } catch (ConstraintViolationException exception) {
            if ("23505".equals(exception.getSQLState())) {
                throw new GameAlreadyExistsException(
                        "Game with id " + requestDto.getId() + " already exists.");
            }
            throw exception;
        }
        return gameMapper.toDto(game);
    }
    
    @Override
    @Transactional
    public GameResponseDto updateGameById(Long gameId, UpdateGameRequestDto requestDto) {
        Game game = getGameOrThrow(gameId);
        
        gameMapper.updateGame(requestDto, game);
        return gameMapper.toDto(game);
    }
    
    @Override
    public void deleteGameById(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new EntityNotFoundException("Game with id " + gameId + " does not exist");
        }
        gameRepository.deleteById(gameId);
    }
    
    private Game getGameOrThrow(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find game by id: " + id)
                );
    }
}
