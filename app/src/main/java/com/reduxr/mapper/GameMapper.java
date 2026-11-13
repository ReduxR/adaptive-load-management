package com.reduxr.mapper;

import com.reduxr.config.MapperConfig;
import com.reduxr.dto.GameResponseDto;
import com.reduxr.model.Game;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface GameMapper {
    GameResponseDto toDto(Game game);
}
