package balbucio.browser4j.bridge.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Enhanced JS message model to support both legacy events and modular calls.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JSMessage {
    // Legacy support
    public String event;
    public Object data;

    // Modular call support
    public String module;
    public String method;
    public List<Object> params; // Using List for Jackson argument matching

    // Security token propagation
    public String bridgeToken;

    public JSMessage() {}

    /** Constructor for legacy and event-based messages. */
    public JSMessage(String event, Object data) {
        this.event = event;
        this.data = data;
    }

    /** Constructor for modular calls. */
    public JSMessage(String module, String method, List<Object> params) {
        this.module = module;
        this.method = method;
        this.params = params;
    }

    /** Checks if this message is a modular call. */
    public boolean isModuleCall() {
        return module != null && method != null;
    }
}
