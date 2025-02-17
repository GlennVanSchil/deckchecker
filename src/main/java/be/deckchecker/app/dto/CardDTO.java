
package be.deckchecker.app.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * The {@link CardDTO}
 */
@Setter
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CardDTO {

    private String id;
    private String cardColor;
    private String cardNumber;
    private int cardEnergyCost;
    private String cardFrontName;
    private String cardFrontPower;
    private String cardFrontTrait;
    private String cardBackName;
    private String cardBackPower;
    private String cardBackTrait;
    private String cardFrontSkill;
    private String cardBackSkill;
    private String cardRarity;
    private String cardType;
    private String cardComboPower;
    private int viewCount;
    private String cardSeries;
    private Object sort;
    private boolean isBanned;
    private boolean isLimited;
    private boolean hasErrata;
    private int limitedTo;
    private Object errataBack;
    private String errataFront;
    private String digitalCardCode;
    private String cardFrontSkillUnstyled;
    private String cardBackSkillUnstyled;
    private int variantOf;

}
