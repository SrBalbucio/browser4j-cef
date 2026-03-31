package tests.junittests.bridge;

import balbucio.browser4j.bridge.serialization.JsonSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSerializerTest {

    private final JsonSerializer serializer = new JsonSerializer();

    // ── serialize ─────────────────────────────────────────────────────────────

    @Test
    void shouldSerializeStringToJsonString() throws JsonProcessingException {
        assertEquals("\"hello\"", serializer.serialize("hello"));
    }

    @Test
    void shouldSerializeNullToJsonNull() throws JsonProcessingException {
        assertEquals("null", serializer.serialize(null));
    }

    @Test
    void shouldSerializeIntegerToJsonNumber() throws JsonProcessingException {
        assertEquals("42", serializer.serialize(42));
    }

    @Test
    void shouldSerializeListToJsonArray() throws JsonProcessingException {
        String json = serializer.serialize(List.of("a", "b", "c"));
        assertEquals("[\"a\",\"b\",\"c\"]", json);
    }

    @Test
    void shouldSerializeMapToJsonObject() throws JsonProcessingException {
        String json = serializer.serialize(Map.of("key", "value"));
        assertTrue(json.contains("\"key\""));
        assertTrue(json.contains("\"value\""));
    }

    @Test
    void shouldEscapeSpecialCharactersInStrings() throws JsonProcessingException {
        String result = serializer.serialize("line1\nline2");
        assertTrue(result.contains("\\n"), "Newline should be escaped as \\n");
    }

    // ── deserialize ───────────────────────────────────────────────────────────

    @Test
    void shouldDeserializeJsonStringToJavaString() throws JsonProcessingException {
        assertEquals("hello", serializer.deserialize("\"hello\"", String.class));
    }

    @Test
    void shouldDeserializeJsonNumberToInteger() throws JsonProcessingException {
        assertEquals(42, serializer.deserialize("42", Integer.class));
    }

    @Test
    void shouldDeserializeJsonObjectToMap() throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = serializer.deserialize("{\"name\":\"browser4j\"}", Map.class);
        assertEquals("browser4j", map.get("name"));
    }

    @Test
    void shouldThrowOnInvalidJson() {
        assertThrows(JsonProcessingException.class,
                () -> serializer.deserialize("not-valid-json", Map.class));
    }

    // ── roundtrip ─────────────────────────────────────────────────────────────

    @Test
    void shouldRoundtripStringSerializationCorrectly() throws JsonProcessingException {
        String original = "browser4j/test key with \"quotes\"";
        String json = serializer.serialize(original);
        String recovered = serializer.deserialize(json, String.class);
        assertEquals(original, recovered);
    }

    @Test
    void shouldExposeObjectMapper() {
        assertNotNull(serializer.getMapper());
    }
}
