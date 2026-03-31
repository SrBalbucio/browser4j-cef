package balbucio.browser4j.network.interception;

import java.util.Map;
import balbucio.browser4j.network.api.Response;

public class RequestDecision {
    private final boolean allowed;
    private final Map<String, String> modifiedHeaders;
    private final byte[] modifiedBody;
    private final Response mockedResponse;

    private RequestDecision(boolean allowed, Map<String, String> modifiedHeaders, byte[] modifiedBody, Response mockedResponse) {
        this.allowed = allowed;
        this.modifiedHeaders = modifiedHeaders;
        this.modifiedBody = modifiedBody;
        this.mockedResponse = mockedResponse;
    }

    public static RequestDecision allow() {
        return new RequestDecision(true, null, null, null);
    }

    public static RequestDecision block() {
        return new RequestDecision(false, null, null, null);
    }

    public static RequestDecision modify(Map<String, String> headers, byte[] body) {
        return new RequestDecision(true, headers, body, null);
    }

    public static RequestDecision mock(Response response) {
        return new RequestDecision(true, null, null, response);
    }

    public boolean isAllowed() { return allowed; }
    public Map<String, String> getModifiedHeaders() { return modifiedHeaders; }
    public byte[] getModifiedBody() { return modifiedBody; }
    public Response getMockedResponse() { return mockedResponse; }
}

