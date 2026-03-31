package balbucio.browser4j.bridge.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializer {
    private final ObjectMapper mapper;

    public JsonSerializer() {
        this.mapper = new ObjectMapper();
    }
    
    public String serialize(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    public <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
