package be.deckchecker.app.service.impl;

import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.service.DeckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
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
        try (InputStream inputStream = openInputStream(filename)) {
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

                    if (line.startsWith("#")) {
                        log.debug("Skipping comment line {}", lineIndex);
                        continue;
                    }

                    String[] parts = line.split("\\s+", 3);
                    if (parts.length < 1) {
                        log.debug("Skipping malformed line in line {}", lineIndex);
                        continue;
                    }

                    if (Character.isDigit(parts[0].charAt(0))) {
                        if (parts.length < 2) {
                            log.warn("Skipping malformed regular card line {}: {}", lineIndex, line);
                            continue;
                        }
                        try {
                            int quantity = Integer.parseInt(parts[0]);
                            String cardId = parts[1];
                            deckCards.add(new DeckCardDTO(cardId, quantity));
                        } catch (NumberFormatException e) {
                            log.warn("Skipping line with invalid quantity {}: {}", lineIndex, line);
                        }
                    } else {
                        String cardId = parts[0];
                        deckCards.add(new DeckCardDTO(cardId, 1));
                    }
                }
            }
            return deckCards;
        }
    }

    private InputStream openInputStream(String filename) throws IOException {
        Path path = Path.of(filename);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
