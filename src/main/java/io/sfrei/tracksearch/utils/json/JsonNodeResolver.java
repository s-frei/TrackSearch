/*
 * Copyright (C) 2023 s-frei (sfrei.io)
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class JsonNodeResolver {


    private final JsonNode node;

    @Getter
    private final boolean resolved;

    public JsonNode node() {
        return node;
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

    protected ArrayNode toArrayNode() {
        return (ArrayNode) node;
    }

    public String asString(final JsonNode node) {
        return nodeIsNull(node) ? null : node.asText();
    }

    protected Long getAsLong(final JsonNode node) {
        return nodeIsNull(node) ? null : node.asLong();
    }

    protected JsonNode atIndex(final int index) {
        return nodeIsNull() ? null : node.get(index);
    }

}
