/*
 * Copyright (C) 2024 s-frei (sfrei.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sfrei.tracksearch.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@SuppressWarnings("unused")
@Slf4j
public class JsonElement extends JsonNodeResolver {

    public JsonElement(JsonNode node, boolean resolved) {
        super(node, resolved);
    }

    private static JsonElement readTree(final ObjectMapper mapper, final String json) throws JsonProcessingException {
        return new JsonElement(mapper.readTree(json), false);
    }

    public static Optional<JsonElement> readTreeCatching(final ObjectMapper mapper, final String json) {
        try {
            return Optional.of(readTree(mapper, json));
        } catch (JsonProcessingException e) {
            log.error("Error occurred reading JSON: '{}'", json, e);
            return Optional.empty();
        }
    }

    public Optional<JsonElement> reReadTree(final ObjectMapper mapper) {
        return readTreeCatching(mapper, asString(node()));
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

    public JsonElement findElement(final String path) {
        return nextElement(node -> node.findValues(path).stream().findFirst().orElse(null));
    }

    public Stream<JsonElement> arrayElements() {
        return isArray() ? StreamSupport.stream(toArrayNode().spliterator(), false).map(JsonElement::of) : Stream.empty();
    }

    public String asString(final String... paths) {
        return super.asString(paths(paths).node());
    }

    public Long asLong(final String... paths) {
        return getAsLong(paths(paths).node());
    }

    public JsonElement paths(final String... paths) {
        return nextElement(e -> nodeForPaths(paths));
    }

    private JsonNode nodeForPaths(String... paths) {
        if (paths.length == 0)
            return node();

        final AtomicReference<JsonNode> tempNode = new AtomicReference<>(node());

        for (final String path : paths) {
            if (tempNode.get() == null)
                return null;
            tempNode.getAndUpdate(tmp -> tmp.get(path));
        }

        return tempNode.get();
    }

    public JsonElement firstElement() {
        return nextElement(node -> atIndex(0));
    }

    public JsonElement lastForPath(final String path) {
        return nextElement(node -> {
            final List<JsonNode> nodes = node.findParents(path);
            if (node.isEmpty()) return null;
            return nodes.get(nodes.size() - 1).path(path);
        });
    }

    public JsonElement elementAtIndex(final int index) {
        return nextElement(node -> atIndex(index));
    }

    public <T> T map(final ObjectMapper mapper, final Class<T> clazz) throws JsonProcessingException {
        return mapper.treeToValue(node(), clazz);
    }

    public <T> T mapCatching(final ObjectMapper mapper, final Class<T> clazz) {
        try {
            return map(mapper, clazz);
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
        return !nodeIsNull();
    }

    public boolean nodePresent(String path) {
        return JsonElement.of(nodeForPaths(path)).isPresent();
    }

}
