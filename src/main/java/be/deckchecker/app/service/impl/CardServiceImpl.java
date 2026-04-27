package be.deckchecker.app.service.impl;

import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.DeckCheckResultDTO;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.DuplicateCardDTO;
import be.deckchecker.app.dto.MissingCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.dto.VariantGroupOverflowDTO;
import be.deckchecker.app.service.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * The {@link CardServiceImpl} is the default implementation of the {@link CardService}
 */
@Slf4j
@Service
public class CardServiceImpl implements CardService {

    private final Map<String, CardDTO> cardIndex;
    private final Map<String, CardDTO> cardNumberIndex;

    /**
     * Creates a new instance of the {@link CardServiceImpl} class.
     *
     * @param cardIndex The index of cards for easy lookup
     */
    public CardServiceImpl(Map<String, CardDTO> cardIndex) {
        this.cardIndex = cardIndex;
        this.cardNumberIndex = new HashMap<>();

        for (CardDTO card : cardIndex.values()) {
            String cardNumber = normalizeCardNumber(card.getCardNumber());
            cardNumberIndex.put(cardNumber, card);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeckCheckResultDTO findMissingCards(List<OwnedCardDTO> ownedCards, List<DeckCardDTO> deckCards) {
        Map<String, Integer> ownedQuantityByVariantGroup = new HashMap<>();
        List<String> unknownDeckCards = new ArrayList<>();

        for (OwnedCardDTO owned : ownedCards) {
            String cardId = normalizeId(owned.getCardId());
            CardDTO card = cardIndex.get(cardId);
            if (card == null) {
                log.warn("Owned card with id {} was not found in cards.json", owned.getCardId());
                continue;
            }

            String variantGroupId = resolveVariantRootId(card);
            ownedQuantityByVariantGroup.merge(variantGroupId, owned.getQuantity(), Integer::sum);
        }

        Map<String, Integer> neededQuantityByVariantGroup = new HashMap<>();
        Map<String, String> displayCardNumberByVariantGroup = new HashMap<>();
        Map<String, String> displayCardNameByVariantGroup = new HashMap<>();
        Map<String, Integer> unknownQuantityByCardNumber = new HashMap<>();

        for (DeckCardDTO deckCard : deckCards) {
            String cardNumber = normalizeCardNumber(deckCard.getCardId());
            CardDTO card = cardNumberIndex.get(cardNumber);
            if (card == null) {
                unknownDeckCards.add(cardNumber);
                unknownQuantityByCardNumber.merge(cardNumber, deckCard.getQuantity(), Integer::sum);
                continue;
            }

            String variantGroupId = resolveVariantRootId(card);
            neededQuantityByVariantGroup.merge(variantGroupId, deckCard.getQuantity(), Integer::sum);
            displayCardNumberByVariantGroup.putIfAbsent(variantGroupId, cardNumber);
            displayCardNameByVariantGroup.putIfAbsent(variantGroupId, card.getDisplayName());
        }

        List<MissingCardDTO> missingCards = new ArrayList<>();
        int totalDeckCopies = 0;
        int totalOwnedCopiesInDeck = 0;
        int totalMissingCopies = 0;

        for (Map.Entry<String, Integer> deckEntry : neededQuantityByVariantGroup.entrySet()) {
            String variantGroupId = deckEntry.getKey();
            int neededQuantity = deckEntry.getValue();
            int ownedQuantity = ownedQuantityByVariantGroup.getOrDefault(variantGroupId, 0);
            int missingQuantity = Math.max(neededQuantity - ownedQuantity, 0);

            totalDeckCopies += neededQuantity;
            totalOwnedCopiesInDeck += Math.min(ownedQuantity, neededQuantity);
            totalMissingCopies += missingQuantity;

            if (missingQuantity > 0) {
                missingCards.add(new MissingCardDTO(
                        displayCardNumberByVariantGroup.getOrDefault(variantGroupId, variantGroupId),
                        displayCardNameByVariantGroup.getOrDefault(variantGroupId, "Unknown card"),
                        neededQuantity,
                        ownedQuantity,
                        missingQuantity
                ));
            }
        }

        for (Map.Entry<String, Integer> unknownEntry : unknownQuantityByCardNumber.entrySet()) {
            String cardNumber = unknownEntry.getKey();
            int neededQuantity = unknownEntry.getValue();

            totalDeckCopies += neededQuantity;
            totalMissingCopies += neededQuantity;

            missingCards.add(new MissingCardDTO(
                    cardNumber,
                    "Unknown card",
                    neededQuantity,
                    0,
                    neededQuantity
            ));
        }

        missingCards.sort(
                Comparator.comparingInt(MissingCardDTO::getMissingQuantity).reversed()
                        .thenComparing(MissingCardDTO::getCardNumber)
        );

        List<DuplicateCardDTO> duplicateCards = findDuplicateCards(ownedCards);
        int totalDuplicateCopies = duplicateCards.stream()
                .mapToInt(DuplicateCardDTO::getDuplicateQuantity)
                .sum();

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

    @Override
    public List<DuplicateCardDTO> findDuplicateCards(List<OwnedCardDTO> ownedCards) {
        Map<String, Integer> ownedQuantityByCardId = new HashMap<>();
        for (OwnedCardDTO owned : ownedCards) {
            String cardId = normalizeId(owned.getCardId());
            ownedQuantityByCardId.merge(cardId, owned.getQuantity(), Integer::sum);
        }

        List<DuplicateCandidate> candidates = new ArrayList<>();
        Map<String, Integer> duplicateEntriesByVariantGroup = new HashMap<>();

        for (Map.Entry<String, Integer> ownedEntry : ownedQuantityByCardId.entrySet()) {
            String cardId = ownedEntry.getKey();
            int ownedQuantity = ownedEntry.getValue();
            int duplicateQuantity = Math.max(ownedQuantity - 4, 0);

            if (duplicateQuantity <= 0) {
                continue;
            }

            CardDTO card = cardIndex.get(cardId);
            if (card == null) {
                log.warn("Owned card with id {} was not found in cards.json", cardId);
            }

            String cardNumber = card != null ? normalizeCardNumber(card.getCardNumber()) : cardId;
            String cardName = card != null ? card.getDisplayName() : "Unknown card";
            String variantGroupId = card != null ? resolveVariantRootId(card) : cardId;

            CardDTO variantRootCard = cardIndex.get(variantGroupId);
            String variantGroupCardNumber = variantRootCard != null
                    ? normalizeCardNumber(variantRootCard.getCardNumber())
                    : cardNumber;

            candidates.add(new DuplicateCandidate(
                    cardId,
                    cardNumber,
                    cardName,
                    ownedQuantity,
                    duplicateQuantity,
                    variantGroupId,
                    variantGroupCardNumber
            ));
            duplicateEntriesByVariantGroup.merge(variantGroupId, 1, Integer::sum);
        }

        List<DuplicateCardDTO> duplicateCards = new ArrayList<>();
        for (DuplicateCandidate candidate : candidates) {
            int variantGroupSize = duplicateEntriesByVariantGroup.getOrDefault(candidate.variantGroupId(), 1);
            duplicateCards.add(new DuplicateCardDTO(
                    candidate.cardId(),
                    candidate.cardNumber(),
                    candidate.cardName(),
                    candidate.ownedQuantity(),
                    candidate.duplicateQuantity(),
                    candidate.variantGroupCardNumber(),
                    variantGroupSize
            ));
        }

        duplicateCards.sort(
                Comparator.comparing(DuplicateCardDTO::getCardNumber)
                        .thenComparing(DuplicateCardDTO::getCardName)
                        .thenComparing(DuplicateCardDTO::getCardId)
        );

        return duplicateCards;
    }

    @Override
    public List<VariantGroupOverflowDTO> findVariantGroupOverflows(List<OwnedCardDTO> ownedCards) {
        Map<String, Integer> ownedQuantityByCardId = new HashMap<>();
        for (OwnedCardDTO owned : ownedCards) {
            String cardId = normalizeId(owned.getCardId());
            ownedQuantityByCardId.merge(cardId, owned.getQuantity(), Integer::sum);
        }

        Map<String, List<OwnedVariantEntry>> ownedByVariantGroup = new HashMap<>();
        for (Map.Entry<String, Integer> entry : ownedQuantityByCardId.entrySet()) {
            String cardId = entry.getKey();
            int quantity = entry.getValue();
            if (quantity <= 0) {
                continue;
            }

            CardDTO card = cardIndex.get(cardId);
            if (card == null) {
                log.warn("Owned card with id {} was not found in cards.json", cardId);
                continue;
            }

            String variantGroupId = resolveVariantRootId(card);
            String cardNumber = normalizeCardNumber(card.getCardNumber());
            String cardName = card.getDisplayName();
            ownedByVariantGroup.computeIfAbsent(variantGroupId, ignored -> new ArrayList<>())
                    .add(new OwnedVariantEntry(cardNumber, cardName, quantity));
        }

        List<VariantGroupOverflowDTO> overflows = new ArrayList<>();
        for (Map.Entry<String, List<OwnedVariantEntry>> groupEntry : ownedByVariantGroup.entrySet()) {
            String variantGroupId = groupEntry.getKey();
            List<OwnedVariantEntry> entries = groupEntry.getValue();
            int combinedOwned = entries.stream().mapToInt(OwnedVariantEntry::quantity).sum();
            int maxSingleVariantOwned = entries.stream().mapToInt(OwnedVariantEntry::quantity).max().orElse(0);

            // Warning scenario for trading: total variants exceed 4 while each exact variant is not a duplicate.
            if (combinedOwned <= 4 || maxSingleVariantOwned > 4) {
                continue;
            }

            CardDTO root = cardIndex.get(variantGroupId);
            String rootCardNumber = root != null ? normalizeCardNumber(root.getCardNumber()) : variantGroupId;
            String rootCardName = root != null ? root.getDisplayName() : "Unknown card";

            entries.sort(Comparator.comparing(OwnedVariantEntry::cardNumber));
            StringJoiner summary = new StringJoiner(", ");
            for (OwnedVariantEntry entry : entries) {
                summary.add(entry.cardNumber() + " x" + entry.quantity());
            }

            overflows.add(new VariantGroupOverflowDTO(
                    rootCardNumber,
                    rootCardName,
                    combinedOwned,
                    combinedOwned - 4,
                    maxSingleVariantOwned,
                    summary.toString()
            ));
        }

        overflows.sort(
                Comparator.comparingInt(VariantGroupOverflowDTO::getOverflowQuantity).reversed()
                        .thenComparing(VariantGroupOverflowDTO::getVariantGroupCardNumber)
                        .thenComparing(VariantGroupOverflowDTO::getVariantGroupName)
        );

        return overflows;
    }

    private String normalizeCardNumber(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String resolveVariantRootId(CardDTO card) {
        if (card == null || card.getId() == null) {
            return "";
        }

        String currentId = normalizeId(card.getId());
        int guard = 0;
        while (guard < 10) {
            CardDTO current = cardIndex.get(currentId);
            if (current == null || current.getVariantOf() == null) {
                return currentId;
            }

            String nextId = normalizeId(String.valueOf(current.getVariantOf()));
            if (nextId.equals(currentId)) {
                return currentId;
            }

            currentId = nextId;
            guard++;
        }

        return currentId;
    }

    private record DuplicateCandidate(
            String cardId,
            String cardNumber,
            String cardName,
            int ownedQuantity,
            int duplicateQuantity,
            String variantGroupId,
            String variantGroupCardNumber
    ) {
    }

    private record OwnedVariantEntry(
            String cardNumber,
            String cardName,
            int quantity
    ) {
    }
}
