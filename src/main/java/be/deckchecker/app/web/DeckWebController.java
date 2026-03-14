package be.deckchecker.app.web;

import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.dto.WrapperDTO;
import be.deckchecker.app.service.CardService;
import be.deckchecker.app.service.DeckDataProvider;
import be.deckchecker.app.service.DeckService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class DeckWebController {

    private final DeckDataProvider dataProvider;
    private final DeckService deckService;
    private final CardService cardService;

    public DeckWebController(DeckDataProvider dataProvider, DeckService deckService, CardService cardService) {
        this.dataProvider = dataProvider;
        this.deckService = deckService;
        this.cardService = cardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("deckText", loadDefaultDeckText());
        return "index";
    }

    @PostMapping("/check")
    public String checkDeck(@RequestParam("deckText") String deckText, Model model) {
        model.addAttribute("deckText", deckText);

        if (deckText == null || deckText.isBlank()) {
            model.addAttribute("error", "Deck text is empty.");
            return "index";
        }

        try {
            WrapperDTO<OwnedCardDTO> ownedCards = dataProvider.loadOwnedCards();
            List<DeckCardDTO> deckCards = deckService.parseDeckText(deckText);
            model.addAttribute("result", cardService.findMissingCards(ownedCards.getData(), deckCards));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "index";
    }

    private String loadDefaultDeckText() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("deck.txt")) {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
