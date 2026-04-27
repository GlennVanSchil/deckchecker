package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuplicateWithVariantsDTO {
    private String duplicateCardNumber;
    private String duplicateCardName;
    private int duplicateOwnedQuantity;
    private int duplicateQuantity;
    private String variantGroupCardNumber;
    private int combinedOwnedQuantity;
    private int combinedDuplicateQuantity;
    private String additionalOwnedVariantsSummary;
}
