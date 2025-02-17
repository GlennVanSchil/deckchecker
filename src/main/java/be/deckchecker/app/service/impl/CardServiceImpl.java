package be.deckchecker.app.service.impl;

import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.service.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link CardServiceImpl} is the default implementation of the {@link CardService}
 */
@Slf4j
@Service
public class CardServiceImpl implements CardService {

    private Map<String, CardDTO> cardIndex;

    /**
     * Creates a new instance of the {@link CardServiceImpl} class.
     *
     * @param cardIndex The index of cards for easy lookup
     */
    public CardServiceImpl(Map<String, CardDTO> cardIndex) {
        this.cardIndex = cardIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeckCardDTO> findMissingCards(List<OwnedCardDTO> ownedCards, List<DeckCardDTO> deckCards) {
        Map<String, Integer> ownedMap = new HashMap<>();
        for (OwnedCardDTO owned : ownedCards) {
            CardDTO card = cardIndex.get(owned.getCardId());
            ownedMap.put(card.getCardNumber(), owned.getQuantity()); // Assuming OwnedCard has a getCard() method returning CardDTO
        }

        return deckCards.stream()
                .filter(deckCard -> {
                    int ownedQuantity = ownedMap.getOrDefault(deckCard.getCardId(), 0);
                    return ownedQuantity < deckCard.getQuantity();
                })
                .map(deckCard -> new DeckCardDTO(
                        deckCard.getCardId(),
                        deckCard.getQuantity() - ownedMap.getOrDefault(deckCard.getCardId(), 0)
                ))
                .toList();
    }
}
