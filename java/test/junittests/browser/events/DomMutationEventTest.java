package tests.junittests.browser.events;

import balbucio.browser4j.browser.events.DomMutationEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DomMutationEventTest {

    @Test
    void shouldParseMapToEventWithAddedRemovedNodes() {
        Map<String, Object> target = new HashMap<>();
        target.put("tag", "DIV");
        target.put("id", "root");
        target.put("class", "container");
        target.put("outerHTML", "<div id=\"root\">...</div>");

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "childList");
        payload.put("target", target);
        payload.put("attributeName", "class");
        payload.put("oldValue", "old-class");
        payload.put("added", List.of("<span>new</span>"));
        payload.put("removed", List.of("<p>old</p>"));

        DomMutationEvent event = DomMutationEvent.fromMap(payload);

        assertNotNull(event);
        assertEquals("childList", event.getType());
        assertEquals("DIV", event.getTargetTag());
        assertEquals("root", event.getTargetId());
        assertEquals("container", event.getTargetClass());
        assertEquals("<div id=\"root\">...</div>", event.getOuterHTML());
        assertEquals("class", event.getAttributeName());
        assertEquals("old-class", event.getOldValue());
        assertEquals(1, event.getAddedOuterHTML().size());
        assertEquals("<span>new</span>", event.getAddedOuterHTML().get(0));
        assertEquals(1, event.getRemovedOuterHTML().size());
        assertEquals("<p>old</p>", event.getRemovedOuterHTML().get(0));
    }

    @Test
    void shouldHandleNullMapSafely() {
        DomMutationEvent event = DomMutationEvent.fromMap(null);
        assertNull(event);
    }

    @Test
    void shouldUseEmptyListsWhenAddedRemovedAbsent() {
        Map<String, Object> payload = Map.of(
                "type", "attributes",
                "target", Map.of("tag", "SPAN")
        );

        DomMutationEvent event = DomMutationEvent.fromMap(payload);

        assertNotNull(event);
        assertTrue(event.getAddedOuterHTML().isEmpty());
        assertTrue(event.getRemovedOuterHTML().isEmpty());
        assertEquals("SPAN", event.getTargetTag());
    }
}
