package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@Slf4j
public class JsonElement extends JsonNodeResolver {

    public JsonElement(JsonNode node, boolean resolved) {
        super(node, resolved);
    }

    public static JsonElement read(final ObjectMapper mapper, final String json) throws JsonProcessingException {
        return new JsonElement(mapper.readTree(json), false);
    }

    public static Optional<JsonElement> readHandled(final ObjectMapper mapper, final  String json) {
        try {
            return Optional.of(read(mapper, json));
        } catch (JsonProcessingException e) {
            log.error("Error occurred reading JSON: '{}'", json, e);
            return Optional.empty();
        }
    }

    public static JsonElement of(JsonNode node) {
        return new JsonElement(node, false);
    }

    private JsonElement resolved() {
        return new JsonElement(node(), true);
    }

    public JsonElement asUnresolved() {
        return of(node());
    }

    public Stream<JsonElement> elements() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node().elements(), Spliterator.ORDERED), false)
                .map(JsonElement::of);
    }

    public JsonElement firstElementFor(final String path) {
        return nextElement(node -> node.findValues(path).stream().findFirst().orElse(null));
    }

    public JsonElement firstElementWhereNotFound(final String path, final String notPath) {
        if (nodeIsNull(node()))
            return this;

        return nextElement(node ->
                node.findValues(path).stream()
                        .filter(pathNode -> Objects.isNull(pathNode.findValue(notPath)))
                        .findFirst()
                        .orElse(null)
        );
    }

    public Stream<JsonElement> arrayElements() {
        return isArray() ? StreamSupport.stream(arrayNode().spliterator(), false).map(JsonElement::of) : Stream.empty();
    }

    public String fieldAsString(final String... paths) {
        return getAsString(path(paths).node());
    }

    public String fieldAsString() {
        return getAsString();
    }

    public Long fieldAsLong(final String... paths) {
        return getAsLong(path(paths).node());
    }

    public JsonElement path(final String... paths) {
        return nextElement(e -> nodeForPath(paths));
    }

    private JsonNode nodeForPath(String... paths) {
        final AtomicReference<JsonNode> tempNode = new AtomicReference<>(node());

        for (final String path : paths) {
            if (tempNode.get() == null)
                return null;
            tempNode.getAndUpdate(tmp -> tmp.get(path));
        }

        return tempNode.get();
    }

    public JsonElement getFirstField() {
        return nextElement(node -> atIndex(0));
    }

    public JsonElement getAtIndex(final int index) {
        return nextElement(node -> atIndex(index));
    }

    public JsonElement reRead(final ObjectMapper mapper) throws JsonProcessingException {
        return read(mapper, getAsString(node()));
    }

    public <T> T mapToObject(final ObjectMapper mapper, final Class<T> clazz) throws JsonProcessingException {
        return mapper.treeToValue(node(), clazz);
    }

    public <T> T mapToObjectHandled(final ObjectMapper mapper, final Class<T> clazz) {
        try {
            return mapToObject(mapper, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON as {}: '{}'", clazz.getSimpleName(), node(), e);
        }
        return null;
    }

    public JsonElement orElse(JsonElement alternative) {
        if (nodeIsNull() && !isResolved()) {
            return JsonElement.of(alternative.node());
        }
        return resolved();
    }

    private JsonElement nextElement(Function<JsonNode, JsonNode> function) {
        if (nodeIsNull() || isResolved()) {
            return this;
        }
        return JsonElement.of(function.apply(node()));
    }

    public boolean isNull() {
        return nodeIsNull();
    }

    public boolean isPresent() {
        return !isNull();
    }

    public boolean fieldPresent(String field) {
        return JsonElement.of(nodeForPath(field)).isPresent();
    }

}
