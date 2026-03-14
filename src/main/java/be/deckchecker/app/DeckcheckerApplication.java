package be.deckchecker.app;

import be.deckchecker.app.dto.DeckCheckResultDTO;
import be.deckchecker.app.dto.DuplicateCardDTO;
import be.deckchecker.app.dto.MissingCardDTO;
import be.deckchecker.app.dto.WrapperDTO;
import be.deckchecker.app.service.CardService;
import be.deckchecker.app.service.DeckDataProvider;
import be.deckchecker.app.dto.DeckCardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.service.impl.DeckServiceImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class DeckcheckerApplication implements CommandLineRunner {

    private final DeckDataProvider dataProvider;
    private final DeckServiceImpl deckService;
    private final CardService cardService;

    public DeckcheckerApplication(DeckDataProvider dataProvider, DeckServiceImpl deckService, CardService cardService) {
        this.dataProvider = dataProvider;
        this.deckService = deckService;
        this.cardService = cardService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DeckcheckerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String deckFile = readArg(args, "--deck", "deck.txt");

        try {
            WrapperDTO<OwnedCardDTO> ownedCards = dataProvider.loadOwnedCards();
            List<DeckCardDTO> deckCards = deckService.parseDeckFile(deckFile);

            DeckCheckResultDTO result = cardService.findMissingCards(ownedCards.getData(), deckCards);
            printResult(result, deckFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void printResult(DeckCheckResultDTO result, String deckFile) {
        System.out.printf("Deck check%n");
        System.out.printf("Deck file: %s%n", deckFile);
        System.out.printf("Owned source: API/cache%n%n");

        if (!result.getUnknownDeckCards().isEmpty()) {
            System.out.println("Unknown deck card numbers (not found in cards.json):");
            for (String cardNumber : result.getUnknownDeckCards()) {
                System.out.printf(" - %s%n", cardNumber);
            }
            System.out.println();
        }

        if (result.getMissingCards().isEmpty()) {
            System.out.println("You already own all cards needed for this deck.");
        } else {
            System.out.printf("%-12s %-36s %8s %8s %8s%n", "Card", "Name", "Need", "Own", "Missing");
            System.out.println("----------------------------------------------------------------------------");
            for (MissingCardDTO card : result.getMissingCards()) {
                System.out.printf(
                        "%-12s %-36.36s %8d %8d %8d%n",
                        card.getCardNumber(),
                        card.getCardName(),
                        card.getNeededQuantity(),
                        card.getOwnedQuantity(),
                        card.getMissingQuantity()
                );
            }
        }

        System.out.println();
        System.out.printf(
                "Summary: need=%d, own(in-deck)=%d, missing=%d%n",
                result.getTotalDeckCopies(),
                result.getTotalOwnedCopiesInDeck(),
                result.getTotalMissingCopies()
        );

        System.out.println();
        System.out.println("Collection duplicates (>4 copies):");
        if (result.getDuplicateCards().isEmpty()) {
            System.out.println("No duplicates found.");
        } else {
            System.out.printf("%-12s %-36s %8s %11s %8s%n", "Card", "Name", "Owned", "Duplicates", "Versions");
            System.out.println("------------------------------------------------------------------------------");
            for (DuplicateCardDTO card : result.getDuplicateCards()) {
                System.out.printf(
                        "%-12s %-36.36s %8d %11d %8s%n",
                        card.getCardNumber(),
                        card.getCardName(),
                        card.getOwnedQuantity(),
                        card.getDuplicateQuantity(),
                        card.isMultipleVersions() ? card.getVersionCount() : "-"
                );
            }

            List<DuplicateCardDTO> versionCheckCards = result.getDuplicateCards().stream()
                    .filter(DuplicateCardDTO::isMultipleVersions)
                    .toList();
            if (!versionCheckCards.isEmpty()) {
                System.out.println();
                System.out.println("Version check needed before trading/selling these duplicates:");
                System.out.println(
                        versionCheckCards.stream()
                                .map(card -> card.getCardNumber() + " (" + card.getVersionCount() + " versions)")
                                .collect(Collectors.joining(", "))
                );
            }
        }

        System.out.printf(
                "Duplicate summary: cards=%d, duplicate copies=%d%n",
                result.getDuplicateCards().size(),
                result.getTotalDuplicateCopies()
        );
    }

    private String readArg(String[] args, String key, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(key + "=")) {
                return arg.substring((key + "=").length());
            }
        }
        return defaultValue;
    }
}
