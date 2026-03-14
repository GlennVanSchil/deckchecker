package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissingCardDTO {
    private String cardNumber;
    private String cardName;
    private int neededQuantity;
    private int ownedQuantity;
    private int missingQuantity;
}
