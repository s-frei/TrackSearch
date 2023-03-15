package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class JsonNodeResolver {


    private final JsonNode node;
    private final boolean resolved;

    public JsonNode node() {
        return node;
    }

    public boolean isResolved() {
        return resolved;
    }

    protected boolean nodeIsNull(JsonNode node) {
        return node == null || node.isNull();
    }

    protected boolean nodeIsNull() {
        return nodeIsNull(node);
    }

    public boolean isArray() {
        return node.isArray();
    }

    protected ArrayNode arrayNode() {
        return (ArrayNode) node;
    }

    protected String getAsString(final JsonNode node) {
        return nodeIsNull(node) ? null : node.asText();
    }

    protected String getAsString() {
        return getAsString(node);
    }

    protected Long getAsLong(final JsonNode node) {
        return nodeIsNull(node) ? null : node.asLong();
    }

    protected JsonNode atIndex(final int index) {
        return nodeIsNull() ? null : node.get(index);
    }

}
