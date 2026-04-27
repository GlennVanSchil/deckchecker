package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VariantGroupOverflowDTO {
    private String variantGroupCardNumber;
    private String variantGroupName;
    private String variantGroupImgLink;
    private int combinedOwnedQuantity;
    private int overflowQuantity;
    private int maxSingleVariantOwned;
    private String ownedVariantSummary;
}
