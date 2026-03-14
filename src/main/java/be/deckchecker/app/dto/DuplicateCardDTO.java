package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuplicateCardDTO {
    private String cardNumber;
    private String cardName;
    private int ownedQuantity;
    private int duplicateQuantity;
    private boolean multipleVersions;
    private int versionCount;
}
