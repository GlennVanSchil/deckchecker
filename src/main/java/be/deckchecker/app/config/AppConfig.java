/*
 * Copyright (c) 2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package be.deckchecker.app.config;

import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.WrapperDTO;
import be.deckchecker.app.service.DeckDataProvider;
import be.deckchecker.app.util.JsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link AppConfig} is a Spring configuration class
 */
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public Map<String, CardDTO> cardIndex(DeckDataProvider dataProvider) throws IOException {
        WrapperDTO<CardDTO> cards = dataProvider.loadCards();
        Map<String, CardDTO> cardIndex = new HashMap<>();
        for (CardDTO card : cards.getData()) {
            cardIndex.put(card.getId(), card);
        }
        return cardIndex;
    }

    @Bean
    public JsonReader jsonReader(ObjectMapper objectMapper) {
        return new JsonReader(objectMapper);
    }
}
