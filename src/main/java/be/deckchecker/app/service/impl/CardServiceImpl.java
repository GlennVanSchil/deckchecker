package be.deckchecker.app.service.impl;

import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.DeckCheckResultDTO;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.DuplicateCardDTO;
import be.deckchecker.app.dto.MissingCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.service.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link CardServiceImpl} is the default implementation of the {@link CardService}
 */
@Slf4j
@Service
public class CardServiceImpl implements CardService {

    private final Map<String, CardDTO> cardIndex;
    private final Map<String, CardDTO> cardNumberIndex;
    private final Map<String, Integer> cardNumberVersionCount;
    private final Map<String, Integer> cardVariantGroupSizeById;

    /**
     * Creates a new instance of the {@link CardServiceImpl} class.
     *
     * @param cardIndex The index of cards for easy lookup
     */
    public CardServiceImpl(Map<String, CardDTO> cardIndex) {
        this.cardIndex = cardIndex;
        this.cardNumberIndex = new HashMap<>();
        this.cardNumberVersionCount = new HashMap<>();
        this.cardVariantGroupSizeById = new HashMap<>();

        for (CardDTO card : cardIndex.values()) {
            String cardNumber = normalize(card.getCardNumber());
            cardNumberIndex.put(cardNumber, card);
            cardNumberVersionCount.merge(cardNumber, 1, Integer::sum);
        }

        Map<String, Integer> groupSizeByRootId = new HashMap<>();
        for (CardDTO card : cardIndex.values()) {
            String rootId = resolveVariantRootId(card);
            groupSizeByRootId.merge(rootId, 1, Integer::sum);
        }
        for (CardDTO card : cardIndex.values()) {
            String rootId = resolveVariantRootId(card);
            cardVariantGroupSizeById.put(card.getId(), groupSizeByRootId.getOrDefault(rootId, 1));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeckCheckResultDTO findMissingCards(List<OwnedCardDTO> ownedCards, List<DeckCardDTO> deckCards) {
        Map<String, Integer> ownedMap = new HashMap<>();
        Map<String, Integer> ownedVersionCountByCardNumber = new HashMap<>();
        List<String> unknownDeckCards = new ArrayList<>();

        for (OwnedCardDTO owned : ownedCards) {
            CardDTO card = cardIndex.get(owned.getCardId());
            if (card == null) {
                log.warn("Owned card with id {} was not found in cards.json", owned.getCardId());
                continue;
            }

            String cardNumber = normalize(card.getCardNumber());
            ownedMap.merge(cardNumber, owned.getQuantity(), Integer::sum);
            ownedVersionCountByCardNumber.merge(cardNumber, getCardVersionCount(card), Math::max);
        }

        Map<String, Integer> deckMap = new HashMap<>();
        for (DeckCardDTO deckCard : deckCards) {
            String cardNumber = normalize(deckCard.getCardId());
            deckMap.merge(cardNumber, deckCard.getQuantity(), Integer::sum);

            if (!cardNumberIndex.containsKey(cardNumber)) {
                unknownDeckCards.add(cardNumber);
            }
        }

        List<MissingCardDTO> missingCards = new ArrayList<>();
        List<DuplicateCardDTO> duplicateCards = new ArrayList<>();
        int totalDeckCopies = 0;
        int totalOwnedCopiesInDeck = 0;
        int totalMissingCopies = 0;
        int totalDuplicateCopies = 0;

        for (Map.Entry<String, Integer> deckEntry : deckMap.entrySet()) {
            String cardNumber = deckEntry.getKey();
            int neededQuantity = deckEntry.getValue();
            int ownedQuantity = ownedMap.getOrDefault(cardNumber, 0);
            int missingQuantity = Math.max(neededQuantity - ownedQuantity, 0);

            totalDeckCopies += neededQuantity;
            totalOwnedCopiesInDeck += Math.min(ownedQuantity, neededQuantity);
            totalMissingCopies += missingQuantity;

            if (missingQuantity > 0) {
                CardDTO card = cardNumberIndex.get(cardNumber);
                String cardName = card != null ? card.getDisplayName() : "Unknown card";

                missingCards.add(new MissingCardDTO(
                        cardNumber,
                        cardName,
                        neededQuantity,
                        ownedQuantity,
                        missingQuantity
                ));
            }
        }

        missingCards.sort(
                Comparator.comparingInt(MissingCardDTO::getMissingQuantity).reversed()
                        .thenComparing(MissingCardDTO::getCardNumber)
        );

        for (Map.Entry<String, Integer> ownedEntry : ownedMap.entrySet()) {
            String cardNumber = ownedEntry.getKey();
            int ownedQuantity = ownedEntry.getValue();
            int duplicateQuantity = Math.max(ownedQuantity - 4, 0);

            if (duplicateQuantity > 0) {
                CardDTO card = cardNumberIndex.get(cardNumber);
                String cardName = card != null ? card.getDisplayName() : "Unknown card";
                int versionCount = Math.max(
                        cardNumberVersionCount.getOrDefault(cardNumber, 1),
                        ownedVersionCountByCardNumber.getOrDefault(cardNumber, 1)
                );
                boolean multipleVersions = versionCount > 1;

                duplicateCards.add(new DuplicateCardDTO(
                        cardNumber,
                        cardName,
                        ownedQuantity,
                        duplicateQuantity,
                        multipleVersions,
                        versionCount
                ));
                totalDuplicateCopies += duplicateQuantity;
            }
        }

        duplicateCards.sort(
                Comparator.comparingInt(DuplicateCardDTO::getDuplicateQuantity).reversed()
                        .thenComparing(DuplicateCardDTO::getCardNumber)
        );

        return new DeckCheckResultDTO(
                missingCards,
                duplicateCards,
                unknownDeckCards.stream().distinct().sorted().toList(),
                totalDeckCopies,
                totalOwnedCopiesInDeck,
                totalMissingCopies,
                totalDuplicateCopies
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String resolveVariantRootId(CardDTO card) {
        if (card == null || card.getId() == null) {
            return "";
        }

        String currentId = card.getId();
        int guard = 0;
        while (guard < 10) {
            CardDTO current = cardIndex.get(currentId);
            if (current == null || current.getVariantOf() == null) {
                return currentId;
            }

            String nextId = String.valueOf(current.getVariantOf());
            if (nextId.equals(currentId)) {
                return currentId;
            }

            currentId = nextId;
            guard++;
        }

        return currentId;
    }

    private int getCardVersionCount(CardDTO card) {
        if (card == null || card.getId() == null) {
            return 1;
        }
        return cardVariantGroupSizeById.getOrDefault(card.getId(), 1);
    }
}
