package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicReference;

public class JsonUtility {

    public static String getAsString(JsonNode node, String value) {
        return node != null ? getAsText(node.get(value)) : null;
    }

    public static String getAsText(JsonNode node) {
        return node != null ? node.asText() : null;
    }

    public static Long getAsLong(JsonNode node, String value) {
        return node != null ? getAsLong(node.get(value)) : null;
    }

    public static Long getAsLong(JsonNode node) {
        return node != null ? node.asLong() : null;
    }

    public static JsonNode get(JsonNode node, String... route) {
        AtomicReference<JsonNode> tempNode = new AtomicReference<>(node);
        for (String step : route) {
            if (tempNode.get() == null)
                break;

            tempNode.getAndUpdate(tmp -> tmp.get(step));
        }
        return tempNode.get();
    }

    public static JsonNode getFirstField(JsonNode node) {
        return get(node, 0);
    }

    public static JsonNode get(JsonNode node, int index) {
        return node != null ? node.get(index) : null;
    }

}
