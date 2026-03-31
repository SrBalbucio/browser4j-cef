package balbucio.browser4j.network.api;

import balbucio.browser4j.network.interception.RequestDecision;

public interface RequestHandler {
    RequestDecision handle(String url, String method);
}
