package balbucio.browser4j.bridge.api;

/**
 * Marker interface for classes that will be exposed as modules to the JS Bridge.
 */
public interface BridgeModule {
    /**
     * Optional name for the module in JS. Defaults to the class simple name.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
