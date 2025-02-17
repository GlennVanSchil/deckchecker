package be.deckchecker.app.service.impl;

import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.service.DeckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link DeckServiceImpl} is the default implementation of the {@link DeckService}
 */
@Slf4j
@Component
public class DeckServiceImpl implements DeckService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeckCardDTO> parseDeckFile(String filename) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + filename);
            }

            List<DeckCardDTO> deckCards = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                int lineIndex = 0;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        log.debug("Skipping empty line in line {}", lineIndex);
                        continue;
                    }

                    lineIndex++;
                    if (lineIndex == 1) {
                        log.debug("Skipping deck name in line {}", lineIndex);
                        continue;
                    }

                    String[] parts = line.split(" ", 3);
                    if (parts.length < 2) {
                        log.debug("Skipping malformed line in line {}", lineIndex);
                        continue;
                    }

                    if (lineIndex == 2) {
                        log.debug("Adding leader card {}", lineIndex);
                        String cardId = parts[0];
                        deckCards.add(new DeckCardDTO(cardId, 1));
                    } else {
                        try {
                            log.debug("Adding regular cards {}", lineIndex);
                            int quantity = Integer.parseInt(parts[0]);
                            String cardId = parts[1];
                            deckCards.add(new DeckCardDTO(cardId, quantity));
                        } catch (NumberFormatException e) {
                            log.error("Error parsing line {}", lineIndex, e);
                        }
                    }
                }
            }
            return deckCards;
        }
    }
}
