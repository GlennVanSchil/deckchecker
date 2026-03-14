
package be.deckchecker.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    @JsonAlias({"card_name"})
    private String cardName;
    private int cardEnergyCost;
    @JsonAlias({"card_name"})
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
    private Integer variantOf;
    private List<Integer> variants;

    public String getDisplayName() {
        if (cardName != null && !cardName.isBlank()) {
            return cardName;
        }

        if (cardFrontName != null && !cardFrontName.isBlank()) {
            return cardFrontName;
        }

        if (cardBackName != null && !cardBackName.isBlank()) {
            return cardBackName;
        }

        return "Unknown name";
    }
}
