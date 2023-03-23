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

package io.sfrei.tracksearch.utils;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DeserializerUtility {

    public <T> ObjectMapper mapperFor(Class<T> type, JsonDeserializer<? extends T> deserializer) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final SimpleModule simpleModule = new SimpleModule();

        simpleModule.addDeserializer(type, deserializer);
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }

}
