package balbucio.browser4j.bridge.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark public methods in a BridgeModule that should be accessible from JS.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BridgeMethod {
    /**
     * Optional name override for the method in JS.
     */
    String value() default "";
}
