package be.deckchecker.app.service;

import be.deckchecker.app.dto.DeckCardDTO;

import java.io.IOException;
import java.util.List;

/**
 * The {@link DeckService} Holds all business logic regarding decks
 */
public interface DeckService {

    /**
     * Parses the txt file with the deck to a list of {@link DeckCardDTO}
     *
     * @param filename The filename that contains the deck in digital client format
     * @return List of {@link DeckCardDTO}
     * @throws IOException If the application fails to read the file
     */
    List<DeckCardDTO> parseDeckFile(String filename) throws IOException;

    /**
     * Parses deck text content to a list of {@link DeckCardDTO}
     *
     * @param deckText Deck contents in digital client format
     * @return List of {@link DeckCardDTO}
     */
    List<DeckCardDTO> parseDeckText(String deckText);
}
