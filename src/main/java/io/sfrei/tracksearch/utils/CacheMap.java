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

import java.util.LinkedHashMap;
import java.util.Map;

public final class CacheMap<K, V> extends LinkedHashMap<K, V> {

    public static final byte MAX_SIZE = 50;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() >= MAX_SIZE;
    }

}
