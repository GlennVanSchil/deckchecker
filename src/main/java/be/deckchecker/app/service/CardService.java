package be.deckchecker.app.service;

import be.deckchecker.app.dto.DeckCheckResultDTO;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.DuplicateCardDTO;
import be.deckchecker.app.dto.DuplicateWithVariantsDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.dto.VariantGroupOverflowDTO;

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

    /**
     * Returns variant groups where total owned copies exceed 4 while each exact variant stays at or below 4.
     *
     * @param ownedCards already owned cards
     * @return Sorted variant group overflow warnings
     */
    List<VariantGroupOverflowDTO> findVariantGroupOverflows(List<OwnedCardDTO> ownedCards);

    /**
     * Returns exact duplicates where additional variants are also owned, while those additional variants stay at or below 4.
     *
     * @param ownedCards already owned cards
     * @return Sorted duplicate-with-variants details for trading decisions
     */
    List<DuplicateWithVariantsDTO> findDuplicatesWithAdditionalVariants(List<OwnedCardDTO> ownedCards);
}
