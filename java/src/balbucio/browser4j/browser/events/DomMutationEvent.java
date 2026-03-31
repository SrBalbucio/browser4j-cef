package balbucio.browser4j.browser.events;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DomMutationEvent {
    private final String type;
    private final String targetTag;
    private final String targetId;
    private final String targetClass;
    private final String outerHTML;

    private final String attributeName;
    private final String oldValue;

    private final List<String> addedOuterHTML;
    private final List<String> removedOuterHTML;

    public DomMutationEvent(
            String type,
            String targetTag,
            String targetId,
            String targetClass,
            String outerHTML,
            String attributeName,
            String oldValue,
            List<String> addedOuterHTML,
            List<String> removedOuterHTML) {
        this.type = type;
        this.targetTag = targetTag;
        this.targetId = targetId;
        this.targetClass = targetClass;
        this.outerHTML = outerHTML;
        this.attributeName = attributeName;
        this.oldValue = oldValue;
        this.addedOuterHTML = addedOuterHTML != null ? addedOuterHTML : Collections.emptyList();
        this.removedOuterHTML = removedOuterHTML != null ? removedOuterHTML : Collections.emptyList();
    }

    public String getType() {
        return type;
    }

    public String getTargetTag() {
        return targetTag;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getOuterHTML() {
        return outerHTML;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public List<String> getAddedOuterHTML() {
        return addedOuterHTML;
    }

    public List<String> getRemovedOuterHTML() {
        return removedOuterHTML;
    }

    public static DomMutationEvent fromMap(Map<?, ?> map) {
        if (map == null) return null;

        String type = Objects.toString(map.get("type"), null);
        Map<?, ?> target = (Map<?, ?>) map.get("target");
        String targetTag = null;
        String targetId = null;
        String targetClass = null;
        String outerHTML = null;
        if (target != null) {
            targetTag = Objects.toString(target.get("tag"), null);
            targetId = Objects.toString(target.get("id"), null);
            targetClass = Objects.toString(target.get("class"), null);
            outerHTML = Objects.toString(target.get("outerHTML"), null);
        }

        String attributeName = Objects.toString(map.get("attributeName"), null);
        String oldValue = Objects.toString(map.get("oldValue"), null);

        List<String> added = map.get("added") instanceof List ?
                ((List<?>) map.get("added")).stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList()) : Collections.emptyList();
        List<String> removed = map.get("removed") instanceof List ?
                ((List<?>) map.get("removed")).stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList()) : Collections.emptyList();

        return new DomMutationEvent(type, targetTag, targetId, targetClass, outerHTML, attributeName, oldValue, added, removed);
    }

    @Override
    public String toString() {
        return "DomMutationEvent{" +
                "type='" + type + '\'' +
                ", targetTag='" + targetTag + '\'' +
                ", targetId='" + targetId + '\'' +
                ", targetClass='" + targetClass + '\'' +
                ", attributeName='" + attributeName + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", addedOuterHTML=" + addedOuterHTML +
                ", removedOuterHTML=" + removedOuterHTML +
                '}';
    }
}