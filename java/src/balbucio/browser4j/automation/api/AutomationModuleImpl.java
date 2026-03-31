package balbucio.browser4j.automation.api;

import balbucio.browser4j.automation.internal.AutomationScripts;
import balbucio.browser4j.bridge.messaging.JSBridge;
import balbucio.browser4j.browser.input.InputController;
import java.util.Map;
import java.util.concurrent.*;

public class AutomationModuleImpl implements AutomationModule {
    private final JSBridge bridge;
    private final InputController input;
    private final Map<String, CompletableFuture<Void>> waiters = new ConcurrentHashMap<>();

    public AutomationModuleImpl(JSBridge bridge, InputController input) {
        this.bridge = bridge;
        this.input = input;
        this.bridge.addHandler((event, data) -> {
            if ("__automation_wait_success".equals(event) || "__automation_wait_error".equals(event)) {
                Map<String, Object> map = (Map<String, Object>) data;
                String id = (String) map.get("id");
                CompletableFuture<Void> future = waiters.remove(id);
                if (future != null) {
                    if (event.contains("success")) future.complete(null);
                    else future.completeExceptionally(new Exception((String) map.get("error")));
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> click(String selector) {
        return bridge.evaluateJavaScript(String.format(AutomationScripts.GET_ELEMENT_COORDINATES, selector))
            .thenAccept(res -> {
                if (res instanceof Map) {
                    Map<String, Object> coords = (Map<String, Object>) res;
                    int x = ((Double) coords.get("x")).intValue();
                    int y = ((Double) coords.get("y")).intValue();
                    input.sendSyntheticMouseClick(x, y, 1, false);
                    input.sendSyntheticMouseClick(x, y, 1, true);
                } else throw new RuntimeException("Element not found: " + selector);
            });
    }

    @Override
    public CompletableFuture<Void> type(String selector, String text) {
        return click(selector).thenRun(() -> {
            for (char c : text.toCharArray()) {
                input.sendSyntheticKeyPress(java.awt.event.KeyEvent.getExtendedKeyCodeForChar(c), c);
            }
        });
    }

    @Override
    public CompletableFuture<String> getText(String selector) {
        return bridge.evaluateJavaScript(String.format("document.querySelector('%s')?.innerText", selector)).thenApply(String::valueOf).thenApply(v -> "null".equals(v) ? null : v);
    }

    @Override
    public CompletableFuture<String> getAttribute(String selector, String attr) {
        return bridge.evaluateJavaScript(String.format("document.querySelector('%s')?.getAttribute('%s')", selector, attr)).thenApply(String::valueOf).thenApply(v -> "null".equals(v) ? null : v);
    }

    @Override
    public CompletableFuture<Boolean> isVisible(String selector) {
        return bridge.evaluateJavaScript(String.format(AutomationScripts.IS_VISIBLE, selector)).thenApply(v -> (Boolean) v);
    }

    @Override
    public CompletableFuture<Void> waitForSelector(String selector, long timeoutMs) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String id = java.util.UUID.randomUUID().toString();
        waiters.put(id, future);
        bridge.evaluateJavaScript(String.format(AutomationScripts.WAIT_FOR_SELECTOR, selector, timeoutMs, id));
        return future;
    }

    @Override
    public CompletableFuture<Void> scrollTo(String selector) {
        return bridge.evaluateJavaScript(String.format("document.querySelector('%s')?.scrollIntoView({ behavior: 'smooth', block: 'center' })", selector)).thenAccept(r -> {});
    }
}
