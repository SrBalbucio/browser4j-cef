package balbucio.browser4j.network.api;

public interface ResponseHandler {
    void handle(String url, int statusCode, String contentType, byte[] chunk);
}
