package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Spliterator;
import java.util.Spliterators;
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

    public Stream<JsonElement> elements() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.elements(), Spliterator.ORDERED), false)
                .map(JsonElement::new);
    }

    public JsonElement firstElementFor(String path) {
        return node.findValues(path).stream().map(JsonElement::new).collect(Collectors.toList()).get(0);
    }

    public boolean isArray() {
        return node.isArray();
    }

    public Stream<JsonElement> arrayElements() {
        if (isArray()) {
            ArrayNode arrayNode = (ArrayNode) this.node;
            return StreamSupport.stream(arrayNode.spliterator(), false).map(JsonElement::new);
        }
        return Stream.empty();
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

    public <T> T mapToObject(ObjectMapper mapper, Class<T> clazz) throws JsonProcessingException {
        return mapper.treeToValue(node, clazz);
    }

    public boolean isNull() {
        return node == null;
    }

    public boolean present() {
        return node != null;
    }

}
