package balbucio.browser4j.network.protocol;

import balbucio.browser4j.network.api.Response;

public interface ProtocolHandler {
    Response handle(String url);
}
