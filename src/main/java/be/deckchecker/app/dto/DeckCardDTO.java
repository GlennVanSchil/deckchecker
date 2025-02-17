package be.deckchecker.app.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * The {@link DeckCardDTO}
 */
@Setter
@Getter
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeckCardDTO {
    private String cardId;
    private int quantity;

    @Override
    public String toString() {
        return cardId + ": " + quantity;
    }
}
