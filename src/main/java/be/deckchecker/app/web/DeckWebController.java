package be.deckchecker.app.web;

import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.dto.WrapperDTO;
import be.deckchecker.app.service.CardService;
import be.deckchecker.app.service.DeckDataProvider;
import be.deckchecker.app.service.DeckService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class DeckWebController {
    private static final String SESSION_EMAIL = "deckcheckerEmail";
    private static final String SESSION_PASSWORD = "deckcheckerPassword";

    private final DeckDataProvider dataProvider;
    private final DeckService deckService;
    private final CardService cardService;

    public DeckWebController(DeckDataProvider dataProvider, DeckService deckService, CardService cardService) {
        this.dataProvider = dataProvider;
        this.deckService = deckService;
        this.cardService = cardService;
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("deckText", loadDefaultDeckText());
        model.addAttribute("loggedInEmail", session.getAttribute(SESSION_EMAIL));
        return "index";
    }

    @PostMapping("/check")
    public String checkDeck(@RequestParam("deckText") String deckText, Model model, HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("deckText", deckText);
        model.addAttribute("loggedInEmail", session.getAttribute(SESSION_EMAIL));

        if (deckText == null || deckText.isBlank()) {
            model.addAttribute("error", "Deck text is empty.");
            return "index";
        }

        try {
            String email = (String) session.getAttribute(SESSION_EMAIL);
            String password = (String) session.getAttribute(SESSION_PASSWORD);
            DeckDataProvider.AuthContext authContext = dataProvider.authenticate(email, password);
            WrapperDTO<OwnedCardDTO> ownedCards = dataProvider.loadOwnedCards(authContext);
            List<DeckCardDTO> deckCards = deckService.parseDeckText(deckText);
            model.addAttribute("result", cardService.findMissingCards(ownedCards.getData(), deckCards));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session, @ModelAttribute("error") String error) {
        if (isLoggedIn(session)) {
            return "redirect:/";
        }
        model.addAttribute("error", error);
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            dataProvider.authenticate(email, password);
            session.setAttribute(SESSION_EMAIL, email);
            session.setAttribute(SESSION_PASSWORD, password);
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Login failed: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(SESSION_EMAIL) != null && session.getAttribute(SESSION_PASSWORD) != null;
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
