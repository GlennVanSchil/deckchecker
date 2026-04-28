package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnedVariantDisplayDTO {
    private String cardNumber;
    private String cardName;
    private String imgLink;
    private int quantity;
}
