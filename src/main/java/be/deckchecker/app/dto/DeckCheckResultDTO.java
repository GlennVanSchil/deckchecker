package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DeckCheckResultDTO {
    private List<MissingCardDTO> missingCards;
    private List<DuplicateCardDTO> duplicateCards;
    private List<String> unknownDeckCards;
    private int totalDeckCopies;
    private int totalOwnedCopiesInDeck;
    private int totalMissingCopies;
    private int totalDuplicateCopies;
}
