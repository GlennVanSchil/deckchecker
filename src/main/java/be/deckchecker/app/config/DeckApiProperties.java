package be.deckchecker.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "deckchecker.api")
public class DeckApiProperties {
    private String baseUrl = "https://api.deckplanet.net/items";
    private String siteUrl = "https://www.deckplanet.net";
    private String email = "";
    private String password = "";
    private String cacheDir = ".cache";
    private long cacheTtlMinutes = 360;
    private boolean forceRefresh = false;
}
