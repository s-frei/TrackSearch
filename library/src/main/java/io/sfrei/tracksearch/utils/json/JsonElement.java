package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonElement extends JsonUtility {

    private final JsonNode jsonNode;

    public JsonElement(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public String getStringFor(String value) {
        return getStringFor(jsonNode, value);
    }

    public Long getLongFor(String value) {
        return getLongFor(jsonNode, value);
    }

    public JsonNode getNode(String... route) {
        return get(jsonNode, route);
    }

    public JsonElement get(String... route) {
        return new JsonElement(get(jsonNode, route));
    }

    public JsonElement getFirst() {
        return new JsonElement(getFirst(jsonNode));
    }

    public JsonElement get(int index) {
        return new JsonElement(get(jsonNode, index));
    }

    public JsonNode getNode() {
        return jsonNode;
    }

    public boolean isNull() {
        return jsonNode == null;
    }

}
