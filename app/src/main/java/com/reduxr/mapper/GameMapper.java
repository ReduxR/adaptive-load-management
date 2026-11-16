package com.reduxr.mapper;

import com.reduxr.config.MapperConfig;
import com.reduxr.dto.CreateGameRequestDto;
import com.reduxr.dto.GameResponseDto;
import com.reduxr.dto.UpdateGameRequestDto;
import com.reduxr.model.Game;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface GameMapper {
    GameResponseDto toDto(Game game);

    Game toModel(CreateGameRequestDto requestDto);
    
    void updateGame(UpdateGameRequestDto requestDto, @MappingTarget Game game);
}
