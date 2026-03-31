package balbucio.browser4j.network.interception;

public interface RequestInterceptor {
    RequestDecision intercept(String url, String method);
}
