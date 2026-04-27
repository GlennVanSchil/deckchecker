package be.deckchecker.app.service;

import be.deckchecker.app.dto.DeckCheckResultDTO;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.DuplicateCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;

import java.util.List;

/**
 * The {@link CardService} holds all business logic regarding {@link OwnedCardDTO}, {@link DeckCardDTO} and {@link be.deckchecker.app.dto.CardDTO}
 */
public interface CardService {

    /**
     * Returns a list of cards still needed to build the requested deck
     *
     * @param ownedCards already owned cards
     * @param deckCards  Needed cards for the deck
     * @return List of cards you don't own yet or don't have the required amount
     */
    DeckCheckResultDTO findMissingCards(List<OwnedCardDTO> ownedCards, List<DeckCardDTO> deckCards);

    /**
     * Returns duplicate cards where the owned quantity is greater than 4 for an exact card variant.
     *
     * @param ownedCards already owned cards
     * @return Sorted duplicate cards
     */
    List<DuplicateCardDTO> findDuplicateCards(List<OwnedCardDTO> ownedCards);
}
