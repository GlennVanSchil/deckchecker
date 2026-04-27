package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuplicateCardDTO {
    private String cardId;
    private String cardNumber;
    private String cardName;
    private String imgLink;
    private int ownedQuantity;
    private int duplicateQuantity;
    private String variantGroupCardNumber;
    private int variantGroupSize;
}
