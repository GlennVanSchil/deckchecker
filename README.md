# Deckchecker

## Data source

Data is loaded from DeckPlanet API and cached locally.

Config is in [`src/main/resources/application.properties`](src/main/resources/application.properties):

- `deckchecker.api.site-url`
- `deckchecker.api.email`
- `deckchecker.api.password`
- `deckchecker.api.cache-dir`
- `deckchecker.api.cache-ttl-minutes`
- `deckchecker.api.force-refresh`

Provide credentials via environment variables:

```bash
export DECKPLANET_EMAIL="you@example.com"
export DECKPLANET_PASSWORD="your-password"
```

Then run:

```bash
./mvnw -DskipTests package
java -jar target/app-0.0.1-SNAPSHOT.jar
```

Open `http://localhost:8080` and paste your deck list.
