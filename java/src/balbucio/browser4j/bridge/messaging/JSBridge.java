package balbucio.browser4j.bridge.messaging;

import balbucio.browser4j.bridge.annotations.BridgeMethod;
import balbucio.browser4j.bridge.api.BridgeModule;
import balbucio.browser4j.bridge.serialization.JsonSerializer;
import balbucio.browser4j.browser.api.Browser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class JSBridge {
    private static final Logger log = LoggerFactory.getLogger(JSBridge.class);
    private final Browser javaBrowser;
    private final CefMessageRouter msgRouter;
    private final List<MessageHandler> handlers = new ArrayList<>();
    private final Map<String, BridgeModule> modules = new HashMap<>();
    private final JsonSerializer serializer = new JsonSerializer();
    private final String bridgeToken = UUID.randomUUID().toString();
    private final Map<String, CompletableFuture<Object>> pendingEvaluations = new ConcurrentHashMap<>();

    public String getBridgeToken() {
        return bridgeToken;
    }

    public JSBridge(CefClient client, Browser javaBrowser) {
        this.javaBrowser = javaBrowser;
        
        // Internal handler for async js evaluation results
        this.addHandler((event, data) -> {
            if ("__bridge_eval_success".equals(event)) {
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    String id = (String) map.get("id");
                    if (id != null) {
                        CompletableFuture<Object> future = pendingEvaluations.remove(id);
                        if (future != null) {
                            future.complete(map.get("result"));
                        }
                    }
                }
            } else if ("__bridge_eval_error".equals(event)) {
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    String id = (String) map.get("id");
                    if (id != null) {
                        CompletableFuture<Object> future = pendingEvaluations.remove(id);
                        if (future != null) {
                            future.completeExceptionally(new Exception(String.valueOf(map.get("error"))));
                        }
                    }
                }
            }
        });

        // Define default window.bridge bindings
        CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig("bridge", "bridgeCancel");
        this.msgRouter = CefMessageRouter.create(config);
        
        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                try {
                    JSMessage msg = serializer.deserialize(request, JSMessage.class);
                    if (msg == null) {
                        callback.failure(400, "Invalid request format");
                        return true;
                    }

                    if (msg.bridgeToken == null || !bridgeToken.equals(msg.bridgeToken)) {
                        callback.failure(401, "Invalid bridge token");
                        return true;
                    }

                    if (msg.isModuleCall()) {
                        handleModuleCall(msg, callback);
                    } else if (msg.event != null) {
                        for (MessageHandler h : handlers) {
                            h.onMessage(msg.event, msg.data);
                        }
                        callback.success("OK");
                    } else {
                        callback.failure(400, "Missing event or module/method");
                    }
                } catch (Exception e) {
                    log.error("Failed handling JS query: " + request, e);
                    callback.failure(500, e.getMessage());
                }
                return true;
            }
        }, true);
        
        client.addMessageRouter(msgRouter);
    }

    private void handleModuleCall(JSMessage msg, CefQueryCallback callback) {
        BridgeModule module = modules.get(msg.module);
        if (module == null) {
            callback.failure(404, "Module not found: " + msg.module);
            return;
        }

        try {
            Method targetMethod = null;
            for (Method m : module.getClass().getMethods()) {
                if (m.isAnnotationPresent(BridgeMethod.class)) {
                    BridgeMethod ann = m.getAnnotation(BridgeMethod.class);
                    String name = ann.value().isEmpty() ? m.getName() : ann.value();
                    if (name.equals(msg.method)) {
                        targetMethod = m;
                        break;
                    }
                }
            }

            if (targetMethod == null) {
                callback.failure(404, "Method not found: " + msg.method + " in module " + msg.module);
                return;
            }

            // Map arguments with proper type conversion via Jackson
            Object[] args = new Object[targetMethod.getParameterCount()];
            if (msg.params != null) {
                java.lang.reflect.Parameter[] parameters = targetMethod.getParameters();
                for (int i = 0; i < Math.min(args.length, msg.params.size()); i++) {
                    args[i] = serializer.getMapper().convertValue(msg.params.get(i), parameters[i].getType());
                }
            }

            Object result = targetMethod.invoke(module, args);

            if (result instanceof CompletableFuture) {
                ((CompletableFuture<?>) result).thenAccept(asyncRes -> {
                    try {
                        callback.success(serializer.serialize(asyncRes));
                    } catch (Exception e) {
                        callback.failure(500, "Serialization error: " + e.getMessage());
                    }
                }).exceptionally(ex -> {
                    callback.failure(500, "Async execution error: " + ex.getMessage());
                    return null;
                });
            } else {
                callback.success(serializer.serialize(result));
            }
        } catch (Exception e) {
            log.error("Module call invocation failed", e);
            callback.failure(500, "Invocation failed: " + e.getCause().getMessage());
        }
    }
    
    public void registerModule(BridgeModule module) {
        this.modules.put(module.getName(), module);
    }

    public void unregisterModule(String name) {
        this.modules.remove(name);
    }
    
    public void addHandler(MessageHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(MessageHandler handler) {
        this.handlers.remove(handler);
    }

    public void postMessage(String event, Object data) {
        try {
            String jsonPayload = serializer.serialize(new JSMessage(event, data));
            String jsCode = String.format("window.dispatchEvent(new CustomEvent('java-message', { detail: %s }));", jsonPayload);
            CefBrowser cefBrowser = (CefBrowser) javaBrowser.getNativeBrowser();
            if (cefBrowser != null) {
                cefBrowser.executeJavaScript(jsCode, cefBrowser.getURL(), 0);
            }
        } catch (Exception e) {
            log.error("Failed to post message to JS", e);
        }
    }

    public CompletableFuture<Object> evaluateJavaScript(String jsCode) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        String callbackId = UUID.randomUUID().toString();
        pendingEvaluations.put(callbackId, future);

        String wrappedCode = "(async function() {\n" +
                "    try {\n" +
                "        let __res = await (async function() {\n" +
                "            " + jsCode + "\n" +
                "        })();\n" +
                "        window.bridge({request: JSON.stringify({event: '__bridge_eval_success', data: {id: '" + callbackId + "', result: __res}}), onSuccess: function(){}, onFailure: function(){}});\n" +
                "    } catch(e) {\n" +
                "        window.bridge({request: JSON.stringify({event: '__bridge_eval_error', data: {id: '" + callbackId + "', error: String(e)}}), onSuccess: function(){}, onFailure: function(){}});\n" +
                "    }\n" +
                "})();";

        CefBrowser cefBrowser = (CefBrowser) javaBrowser.getNativeBrowser();
        if (cefBrowser != null) {
            cefBrowser.executeJavaScript(wrappedCode, cefBrowser.getURL(), 0);
        } else {
            future.completeExceptionally(new Exception("Native browser not available"));
        }

        return future;
    }

    public String getInjectionScript() {
        StringBuilder sb = new StringBuilder();
        sb.append("(function() {\n");
        sb.append("  if (window.__browser4j_initialized) return;\n");
        sb.append("  window.__browser4j_initialized = true;\n\n");
        sb.append("  const __browser4j_bridge_token = '" + bridgeToken + "';\n");
        sb.append("  Object.defineProperty(window, '__browser4j_bridge_token', { value: __browser4j_bridge_token, writable: false, configurable: false });\n");
        sb.append("  if (typeof window.bridge === 'function' && !window.__browser4j_bridge_safe_installed) {\n");
        sb.append("    const __browser4j_native_bridge = window.bridge;\n");
        sb.append("    const __browser4j_secure_bridge = function(payload) {\n");
        sb.append("      if (!payload || typeof payload !== 'object') return;\n");
        sb.append("      payload.bridgeToken = __browser4j_bridge_token;\n");
        sb.append("      return __browser4j_native_bridge(payload);\n");
        sb.append("    };\n");
        sb.append("    Object.defineProperty(__browser4j_secure_bridge, '__browser4j_is_secure', { value: true, writable: false, configurable: false });\n");
        sb.append("    Object.defineProperty(window, 'bridge', { value: __browser4j_secure_bridge, writable: false, configurable: false });\n");
        sb.append("    window.__browser4j_bridge_safe_installed = true;\n");
        sb.append("  }\n\n");

        for (Map.Entry<String, BridgeModule> entry : modules.entrySet()) {
            String moduleName = entry.getKey();
            BridgeModule module = entry.getValue();
            
            sb.append("  window.").append(moduleName).append(" = {\n");
            
            Method[] methods = module.getClass().getMethods();
            boolean first = true;
            for (Method m : methods) {
                if (m.isAnnotationPresent(BridgeMethod.class)) {
                    if (!first) sb.append(",\n");
                    BridgeMethod ann = m.getAnnotation(BridgeMethod.class);
                    String methodName = ann.value().isEmpty() ? m.getName() : ann.value();
                    
                    sb.append("    ").append(methodName).append(": function() {\n");
                    sb.append("      const args = Array.from(arguments);\n");
                    sb.append("      return new Promise((resolve, reject) => {\n");
                    sb.append("        window.bridge({\n");
                    sb.append("          request: JSON.stringify({ module: '").append(moduleName).append("', method: '").append(methodName).append("', params: args }),\n");
                    sb.append("          onSuccess: function(response) {\n");
                    sb.append("            try { resolve(JSON.parse(response)); } catch(e) { resolve(response); }\n");
                    sb.append("          },\n");
                    sb.append("          onFailure: function(code, msg) { reject({ code, msg }); }\n");
                    sb.append("        });\n");
                    sb.append("      });\n");
                    sb.append("    }");
                    first = false;
                }
            }
            sb.append("\n  };\n");
        }
        sb.append("})();");
        return sb.toString();
    }

    public void dispose() {
        if (msgRouter != null) {
            msgRouter.dispose();
        }
    }
}
