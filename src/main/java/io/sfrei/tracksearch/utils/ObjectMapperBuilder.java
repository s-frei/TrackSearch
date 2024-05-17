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

package io.sfrei.tracksearch.utils;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.NoArgsConstructor;

@NoArgsConstructor(staticName = "create")
public class ObjectMapperBuilder {

    final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleModule simpleModule = new SimpleModule();

    public <T> ObjectMapperBuilder addDeserializer(Class<T> type, JsonDeserializer<T> deserializer) {
        simpleModule.addDeserializer(type, deserializer);
        return this;
    }

    public ObjectMapper get() {
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }

}
