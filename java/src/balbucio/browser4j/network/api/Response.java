package balbucio.browser4j.network.api;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Response {

    private int statusCode;
    private String contentType;
    private byte[] body;
    private Map<String, String> headers = new HashMap<>();

    private Response(int statusCode, String contentType, byte[] body) {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.body = body;
    }

    public static Response mock(int statusCode, String contentType, byte[] body) {
        return new Response(statusCode, contentType, body);
    }

}
