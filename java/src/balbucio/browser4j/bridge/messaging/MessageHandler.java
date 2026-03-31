package balbucio.browser4j.bridge.messaging;

public interface MessageHandler {
    void onMessage(String event, Object data);
}
