package be.deckchecker.app.util;

import be.deckchecker.app.dto.WrapperDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@link JsonReader} deserializes a json file into a list of objects
 */
public class JsonReader {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new instance of the {@link JsonReader} class.
     *
     * @param objectMapper The preconfigured {@link ObjectMapper}
     */
    public JsonReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes a json file into a list of objects
     *
     * @param filename The name of the file to deserialize
     * @param type     The object type to deserialize into
     * @param <T>      The return type after deserializing
     * @return List of deserialized objects
     * @throws IOException When an {@link InputStream} can not be created for the file
     */
    public <T> T readJsonFile(String filename, TypeReference<T> type) throws IOException {
        try (InputStream inputStream = openInputStream(filename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + filename);
            }
            return objectMapper.readValue(inputStream, type);
        }
    }

    private InputStream openInputStream(String filename) throws IOException {
        Path path = Path.of(filename);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        return JsonReader.class.getClassLoader().getResourceAsStream(filename);
    }

}
