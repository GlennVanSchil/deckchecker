package be.deckchecker.app;

import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.WrapperDTO;
import be.deckchecker.app.service.CardService;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.util.JsonReader;
import be.deckchecker.app.service.impl.DeckServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;


@SpringBootApplication
public class DeckcheckerApplication implements CommandLineRunner {

    private JsonReader jsonReader;
    private DeckServiceImpl deckService;
    private CardService cardService;

    public DeckcheckerApplication(JsonReader jsonReader, DeckServiceImpl deckService, CardService cardService) {
        this.jsonReader = jsonReader;
        this.deckService = deckService;
        this.cardService = cardService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DeckcheckerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        try {
            WrapperDTO<OwnedCardDTO> ownedCards = jsonReader.readJsonFile("owned_cards.json", new TypeReference<>() {
            });
            List<DeckCardDTO> deckCards = deckService.parseDeckFile("deck.txt");

            List<DeckCardDTO> missingCards = cardService.findMissingCards(ownedCards.getData(), deckCards);
            for (DeckCardDTO missingCard : missingCards) {
                System.out.println(missingCard);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
