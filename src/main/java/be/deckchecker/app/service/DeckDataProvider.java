package be.deckchecker.app.service;

import be.deckchecker.app.config.DeckApiProperties;
import be.deckchecker.app.dto.CardDTO;
import be.deckchecker.app.dto.OwnedCardDTO;
import be.deckchecker.app.dto.WrapperDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class DeckDataProvider {

    private static final TypeReference<WrapperDTO<CardDTO>> CARD_WRAPPER_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<WrapperDTO<OwnedCardDTO>> OWNED_WRAPPER_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final DeckApiProperties apiProperties;
    private final HttpClient httpClient;
    private String cachedBearerToken;
    private String cachedUserId;

    public DeckDataProvider(ObjectMapper objectMapper, DeckApiProperties apiProperties) {
        this.objectMapper = objectMapper;
        this.apiProperties = apiProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public WrapperDTO<CardDTO> loadCards() throws IOException {
        return readWithCache(
                resolveCachePath("cards_cache.json"),
                buildCardsUri(),
                null,
                CARD_WRAPPER_TYPE
        );
    }

    public WrapperDTO<OwnedCardDTO> loadOwnedCards() throws IOException {
        AuthContext authContext = resolveAuthContext();
        return readWithCache(
                resolveCachePath("owned_cards_cache.json"),
                buildOwnedUri(authContext.userId()),
                authContext.bearerToken(),
                OWNED_WRAPPER_TYPE
        );
    }

    private <T> T readWithCache(Path cachePath, URI uri, String bearerToken, TypeReference<T> typeReference)
            throws IOException {
        if (!apiProperties.isForceRefresh() && isFresh(cachePath)) {
            return readFromCache(cachePath, typeReference);
        }

        try {
            String body = fetch(uri, bearerToken);
            ensureCacheDir(cachePath.getParent());
            Files.writeString(cachePath, body, StandardCharsets.UTF_8);
            return objectMapper.readValue(body, typeReference);
        } catch (Exception e) {
            log.warn("API request failed for {}: {}", uri, e.getMessage());
            if (Files.exists(cachePath)) {
                log.warn("Using stale cache at {}", cachePath);
                return readFromCache(cachePath, typeReference);
            }
            throw new IOException("API fetch failed and no cache/fallback available for " + uri, e);
        }
    }

    private <T> T readFromCache(Path cachePath, TypeReference<T> typeReference) throws IOException {
        String cached = Files.readString(cachePath, StandardCharsets.UTF_8);
        return objectMapper.readValue(cached, typeReference);
    }

    private boolean isFresh(Path cachePath) {
        try {
            if (!Files.exists(cachePath)) {
                return false;
            }
            Instant modifiedAt = Files.getLastModifiedTime(cachePath).toInstant();
            long ageMinutes = Duration.between(modifiedAt, Instant.now()).toMinutes();
            return ageMinutes <= apiProperties.getCacheTtlMinutes();
        } catch (IOException e) {
            return false;
        }
    }

    private void ensureCacheDir(Path directory) throws IOException {
        if (directory != null) {
            Files.createDirectories(directory);
        }
    }

    private String fetch(URI uri, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Referer", "https://www.deckplanet.net/")
                .header("Origin", "https://www.deckplanet.net");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private AuthContext resolveAuthContext() throws IOException {
        if (cachedBearerToken != null && !cachedBearerToken.isBlank()
                && cachedUserId != null && !cachedUserId.isBlank()) {
            return new AuthContext(cachedBearerToken, cachedUserId);
        }
        if (apiProperties.getEmail() == null || apiProperties.getEmail().isBlank()
                || apiProperties.getPassword() == null || apiProperties.getPassword().isBlank()) {
            throw new IllegalArgumentException("No credentials configured. Set deckchecker.api.email/password.");
        }

        AuthContext authContext = loginAndFetchAuthContext();
        cachedBearerToken = authContext.bearerToken();
        cachedUserId = authContext.userId();
        return authContext;
    }

    private AuthContext loginAndFetchAuthContext() throws IOException {
        try {
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            HttpClient authClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .cookieHandler(cookieManager)
                    .build();

            String siteUrl = trimTrailingSlash(apiProperties.getSiteUrl());
            String csrfToken = fetchCsrfToken(authClient, siteUrl);
            performCredentialsLogin(authClient, siteUrl, csrfToken);
            return fetchAuthFromSession(authClient, siteUrl);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Credential login failed: " + e.getMessage(), e);
        }
    }

    private String fetchCsrfToken(HttpClient authClient, String siteUrl) throws IOException, InterruptedException {
        URI csrfUri = URI.create(siteUrl + "/api/auth/csrf");
        HttpRequest request = HttpRequest.newBuilder(csrfUri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Referer", siteUrl + "/fusion_world/login")
                .header("Origin", siteUrl)
                .build();

        HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("CSRF fetch failed: HTTP " + response.statusCode());
        }

        JsonNode node = objectMapper.readTree(response.body());
        String csrfToken = node.path("csrfToken").asText("");
        if (csrfToken.isBlank()) {
            throw new IOException("CSRF token missing in /api/auth/csrf response.");
        }
        return csrfToken;
    }

    private void performCredentialsLogin(HttpClient authClient, String siteUrl, String csrfToken)
            throws IOException, InterruptedException {
        URI callbackUri = URI.create(siteUrl + "/api/auth/callback/credentials");
        String callbackUrl = siteUrl + "/fusion_world/login";

        String body = "email=" + encode(apiProperties.getEmail())
                + "&password=" + encode(apiProperties.getPassword())
                + "&csrfToken=" + encode(csrfToken)
                + "&callbackUrl=" + encode(callbackUrl)
                + "&json=true";

        HttpRequest request = HttpRequest.newBuilder(callbackUri)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("Referer", callbackUrl)
                .header("Origin", siteUrl)
                .build();

        HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new IOException("Credential callback failed: HTTP " + response.statusCode());
        }
    }

    private AuthContext fetchAuthFromSession(HttpClient authClient, String siteUrl) throws IOException, InterruptedException {
        URI sessionUri = URI.create(siteUrl + "/api/auth/session");
        HttpRequest request = HttpRequest.newBuilder(sessionUri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Referer", siteUrl + "/fusion_world/login")
                .header("Origin", siteUrl)
                .build();

        HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Session fetch failed: HTTP " + response.statusCode());
        }

        JsonNode sessionNode = objectMapper.readTree(response.body());
        JsonNode userNode = sessionNode.path("user");

        String userId = userNode.path("id").asText("");
        String accessToken = userNode.path("accessToken").asText("");

        if (userId.isBlank()) {
            throw new IOException("No user.id found in /api/auth/session response.");
        }
        if (accessToken.isBlank()) {
            throw new IOException("No user.accessToken found in /api/auth/session response.");
        }
        return new AuthContext(accessToken, userId);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://www.deckplanet.net";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private URI buildCardsUri() {
        String filter = "{\"status\":{\"_eq\":\"published\"}}";
        return URI.create(apiProperties.getBaseUrl() + "/fusion_world_cards?filter="
                + encode(filter) + "&limit=-1");
    }

    private URI buildOwnedUri(String userId) {
        String filter = "{\"user_id\":{\"_eq\":\"" + userId + "\"}}";
        return URI.create(apiProperties.getBaseUrl() + "/fusion_world_user_collection?filter="
                + encode(filter) + "&limit=-1");
    }

    private Path resolveCachePath(String filename) {
        return Path.of(apiProperties.getCacheDir(), filename);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record AuthContext(String bearerToken, String userId) {
    }
}
