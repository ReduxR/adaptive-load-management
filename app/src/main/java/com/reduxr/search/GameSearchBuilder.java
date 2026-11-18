package com.reduxr.search;

import com.reduxr.dto.GameSearchParametersDto;
import com.reduxr.exception.InvalidRequestException;
import com.reduxr.model.Game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSearchBuilder {
    private final JdbcClient client;

    public Page<Game> searchGamesByParams(GameSearchParametersDto parametersDto) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        int size = parametersDto.getSize() == null ? 20 : parametersDto.getSize();
        int page = parametersDto.getPage() == null ? 0 : parametersDto.getPage();

        if (size <= 0 || size > 100) {
            throw new InvalidRequestException("Size must be between 1 and 100");
        }
        if (page < 0) {
            throw new InvalidRequestException("Page must not be negative");
        }

        params.put("limit", size);
        params.put("offset", (long) page * size);

        if (parametersDto.getName() != null && !parametersDto.getName().trim().isEmpty()) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + parametersDto.getName().trim() + "%");
        }
        if (parametersDto.getPrice() != null) {
            conditions.add("price = :price");
            params.put("price", parametersDto.getPrice());
        }
        if (parametersDto.getReleaseDate() != null) {
            conditions.add("release_date <= :releaseDateTo");
            params.put("releaseDateTo", parametersDto.getReleaseDate());
        }

        String where = conditions.isEmpty()
                ? ""
                : " WHERE " + String.join(" AND ", conditions);

        String query = """
                SELECT app_id AS id, name, release_date, price, short_description
                FROM games
                """ + where + " ORDER BY app_id LIMIT :limit OFFSET :offset";

        String countGamesQuery = "SELECT COUNT(*) FROM games" + where;

        List<Game> gamesList = client.sql(query)
                .params(params)
                .query(Game.class)
                .list();

        Long count = client.sql(countGamesQuery)
                .params(params)
                .query(Long.class)
                .single();

        return new PageImpl<>(
                gamesList,
                PageRequest.of(page, size),
                count
        );
    }
}
