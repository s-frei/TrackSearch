package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicReference;

public class JsonUtility {

    public static String getAsString(final JsonNode node, final String value) {
        return node != null ? getAsText(node.get(value)) : null;
    }

    public static String getAsText(final JsonNode node) {
        return node != null ? node.asText() : null;
    }

    public static Long getAsLong(final JsonNode node, final String value) {
        return node != null ? getAsLong(node.get(value)) : null;
    }

    public static Long getAsLong(final JsonNode node) {
        return node != null ? node.asLong() : null;
    }

    public static JsonNode get(final JsonNode node, final String... route) {
        final AtomicReference<JsonNode> tempNode = new AtomicReference<>(node);
        for (final String step : route) {
            if (tempNode.get() == null)
                break;

            tempNode.getAndUpdate(tmp -> tmp.get(step));
        }
        return tempNode.get();
    }

    public static JsonNode getFirstField(final JsonNode node) {
        return get(node, 0);
    }

    public static JsonNode get(final JsonNode node, final int index) {
        return node != null ? node.get(index) : null;
    }

}
