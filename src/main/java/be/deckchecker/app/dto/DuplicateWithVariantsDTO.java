package be.deckchecker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuplicateWithVariantsDTO {
    private String duplicateCardNumber;
    private String duplicateCardName;
    private String duplicateImgLink;
    private int duplicateOwnedQuantity;
    private int duplicateQuantity;
    private String variantGroupCardNumber;
    private String variantGroupImgLink;
    private int combinedOwnedQuantity;
    private int combinedDuplicateQuantity;
    private String additionalOwnedVariantsSummary;
}
