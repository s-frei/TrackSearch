package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicReference;

public class JsonUtility {

    public static String getStringFor(JsonNode node, String value) {
        return node != null ? getStringFor(node.get(value)) : null;
    }

    public static String getStringFor(JsonNode node) {
        return node != null ? node.asText() : null;
    }

    public static Long getLongFor(JsonNode node, String value) {
        return node != null ? getLongFor(node.get(value)) : null;
    }

    public static Long getLongFor(JsonNode node) {
        return node != null ? node.asLong() : null;
    }

    public static boolean getStringForContaining(JsonNode node, String value, String containing) {
        String jsonString = getStringFor(node, value);
        return jsonString != null && jsonString.contains(containing);
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

    public static JsonNode getFirst(JsonNode node) {
        return get(node, 0);
    }

    public static JsonNode get(JsonNode node, int index) {
        return node != null ? node.get(index) : null;
    }

}
