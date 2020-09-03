package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor(staticName = "of")
public class JsonElement extends JsonUtility {

    @Getter
    private final JsonNode node;

    public static JsonElement read(ObjectMapper mapper, String jsonString) throws JsonProcessingException {
        return new JsonElement(mapper.readTree(jsonString));
    }

    private List<JsonElement> elements(Stream<JsonNode> nodeStream) {
        return nodeStream.map(JsonElement::new).collect(Collectors.toList());
    }

    public List<JsonElement> elements(String path) {
        return elements(node.findValues(path).stream());
    }

    public JsonElement firstElement(String path) {
        return elements(path).get(0);
    }

    public List<JsonElement> arrayElements() {
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) this.node;
            return elements(StreamSupport.stream(arrayNode.spliterator(), false));
        }
        return Collections.emptyList();
    }

    public String getAsString(String path) {
        return getAsString(node, path);
    }

    public String getAsString(String... paths) {
        for (String value : paths) {
            String result = getAsString(node, value);
            if (result != null)
                return result;
        }
        return null;
    }

    public Long getLongFor(String path) {
        return getAsLong(node, path);
    }

    public JsonElement get(String... route) {
        return new JsonElement(get(node, route));
    }

    public JsonElement getFirstField() {
        return new JsonElement(getFirstField(node));
    }

    public JsonElement getIndex(int index) {
        return new JsonElement(get(node, index));
    }

    public JsonElement orElseGet(Supplier<JsonElement> supplier) {
        return node != null ? new JsonElement(node) : supplier.get();
    }

    public JsonElement reRead(ObjectMapper mapper) throws JsonProcessingException {
        return new JsonElement(mapper.readTree(getAsText(node)));
    }

    public boolean isNull() {
        return node == null;
    }

    public boolean present() {
        return node != null;
    }

}
