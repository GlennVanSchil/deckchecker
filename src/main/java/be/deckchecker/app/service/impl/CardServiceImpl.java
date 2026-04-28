package be.deckchecker.app.service.impl;

import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.DeckCheckResultDTO;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.DuplicateCardDTO;
import be.deckchecker.app.dto.DuplicateWithVariantsDTO;
import be.deckchecker.app.dto.MissingCardDTO;
import be.deckchecker.app.dto.OwnedVariantDisplayDTO;
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
        Map<String, String> displayImgLinkByVariantGroup = new HashMap<>();
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
            displayImgLinkByVariantGroup.putIfAbsent(variantGroupId, card.getImgLink());
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
                        displayImgLinkByVariantGroup.getOrDefault(variantGroupId, null),
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
                    null,
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
                    card != null ? card.getImgLink() : null,
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
                    candidate.imgLink(),
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
                    .add(new OwnedVariantEntry(cardNumber, cardName, card.getImgLink(), quantity));
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
            List<OwnedVariantDisplayDTO> ownedVariants = entries.stream()
                    .map(entry -> new OwnedVariantDisplayDTO(
                            entry.cardNumber(),
                            entry.cardName(),
                            entry.imgLink(),
                            entry.quantity()
                    ))
                    .toList();

            overflows.add(new VariantGroupOverflowDTO(
                    rootCardNumber,
                    rootCardName,
                    root != null ? root.getImgLink() : null,
                    combinedOwned,
                    combinedOwned - 4,
                    maxSingleVariantOwned,
                    summary.toString(),
                    ownedVariants
            ));
        }

        overflows.sort(
                Comparator.comparingInt(VariantGroupOverflowDTO::getOverflowQuantity).reversed()
                        .thenComparing(VariantGroupOverflowDTO::getVariantGroupCardNumber)
                        .thenComparing(VariantGroupOverflowDTO::getVariantGroupName)
        );

        return overflows;
    }

    @Override
    public List<DuplicateWithVariantsDTO> findDuplicatesWithAdditionalVariants(List<OwnedCardDTO> ownedCards) {
        Map<String, Integer> ownedQuantityByCardId = new HashMap<>();
        for (OwnedCardDTO owned : ownedCards) {
            String cardId = normalizeId(owned.getCardId());
            ownedQuantityByCardId.merge(cardId, owned.getQuantity(), Integer::sum);
        }

        Map<String, List<OwnedVariantDetailedEntry>> ownedByVariantGroup = new HashMap<>();
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
                    .add(new OwnedVariantDetailedEntry(cardId, cardNumber, cardName, card.getImgLink(), quantity));
        }

        List<DuplicateWithVariantsDTO> rows = new ArrayList<>();
        for (Map.Entry<String, List<OwnedVariantDetailedEntry>> groupEntry : ownedByVariantGroup.entrySet()) {
            String variantGroupId = groupEntry.getKey();
            List<OwnedVariantDetailedEntry> entries = groupEntry.getValue();

            int combinedOwned = entries.stream().mapToInt(OwnedVariantDetailedEntry::quantity).sum();
            int combinedDuplicateQuantity = Math.max(combinedOwned - 4, 0);
            if (combinedDuplicateQuantity <= 0) {
                continue;
            }

            CardDTO root = cardIndex.get(variantGroupId);
            String rootCardNumber = root != null ? normalizeCardNumber(root.getCardNumber()) : variantGroupId;

            for (OwnedVariantDetailedEntry duplicateEntry : entries) {
                if (duplicateEntry.quantity() <= 4) {
                    continue;
                }

                List<OwnedVariantDetailedEntry> additionalVariants = entries.stream()
                        .filter(entry -> !entry.cardId().equals(duplicateEntry.cardId()))
                        .filter(entry -> entry.quantity() > 0)
                        .toList();

                if (additionalVariants.isEmpty()) {
                    continue;
                }

                boolean additionalVariantsContainDuplicates = additionalVariants.stream()
                        .anyMatch(entry -> entry.quantity() > 4);
                if (additionalVariantsContainDuplicates) {
                    continue;
                }

                List<OwnedVariantDetailedEntry> sortedAdditionalVariants = new ArrayList<>(additionalVariants);
                sortedAdditionalVariants.sort(Comparator.comparing(OwnedVariantDetailedEntry::cardNumber));

                StringJoiner additionalSummary = new StringJoiner(", ");
                for (OwnedVariantDetailedEntry additionalVariant : sortedAdditionalVariants) {
                    additionalSummary.add(additionalVariant.cardNumber() + " x" + additionalVariant.quantity());
                }

                String variantGroupImgLink = root != null ? root.getImgLink() : null;
                if (isBlank(variantGroupImgLink) || variantGroupImgLink.equals(duplicateEntry.imgLink())) {
                    variantGroupImgLink = sortedAdditionalVariants.stream()
                            .map(OwnedVariantDetailedEntry::imgLink)
                            .filter(this::isNotBlank)
                            .filter(img -> !img.equals(duplicateEntry.imgLink()))
                            .findFirst()
                            .orElse(variantGroupImgLink);
                }

                rows.add(new DuplicateWithVariantsDTO(
                        duplicateEntry.cardNumber(),
                        duplicateEntry.cardName(),
                        duplicateEntry.imgLink(),
                        duplicateEntry.quantity(),
                        duplicateEntry.quantity() - 4,
                        rootCardNumber,
                        variantGroupImgLink,
                        combinedOwned,
                        combinedDuplicateQuantity,
                        additionalSummary.toString(),
                        entries.stream()
                                .sorted(Comparator.comparing(OwnedVariantDetailedEntry::cardNumber))
                                .map(entry -> new OwnedVariantDisplayDTO(
                                        entry.cardNumber(),
                                        entry.cardName(),
                                        entry.imgLink(),
                                        entry.quantity()
                                ))
                                .toList()
                ));
            }
        }

        rows.sort(
                Comparator.comparing(DuplicateWithVariantsDTO::getDuplicateCardNumber)
                        .thenComparing(DuplicateWithVariantsDTO::getDuplicateCardName)
                        .thenComparing(Comparator.comparingInt(DuplicateWithVariantsDTO::getDuplicateQuantity).reversed())
        );

        return rows;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    private record DuplicateCandidate(
            String cardId,
            String cardNumber,
            String cardName,
            String imgLink,
            int ownedQuantity,
            int duplicateQuantity,
            String variantGroupId,
            String variantGroupCardNumber
    ) {
    }

    private record OwnedVariantEntry(
            String cardNumber,
            String cardName,
            String imgLink,
            int quantity
    ) {
    }

    private record OwnedVariantDetailedEntry(
            String cardId,
            String cardNumber,
            String cardName,
            String imgLink,
            int quantity
    ) {
    }
}
