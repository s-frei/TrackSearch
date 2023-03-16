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

package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.QueryType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class BaseTrackList<T extends Track> implements TrackList<T> {

    private final List<T> tracks;

    private QueryType queryType = QueryType.UNKNOWN;
    private final Map<String, String> queryInformation;

    public BaseTrackList() {
        this.tracks = new ArrayList<>();
        this.queryInformation = new HashMap<>();
    }

    @Override
    public void mergeIn(BaseTrackList<T> from) {
        this.tracks.addAll(from.tracks);
        this.queryInformation.putAll(from.queryInformation);
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public BaseTrackList<T> setPagingValues(String positionKey, int position, String offsetKey, int offset) {
        queryInformation.putAll(Map.of(positionKey, String.valueOf(position), offsetKey, String.valueOf(offset)));
        return this;
    }

    public void addQueryInformationValue(String key, int value) {
        queryInformation.put(key, String.valueOf(value));
    }

    @Override
    public Integer getQueryInformationIntValue(String key) {
        return Integer.parseInt(queryInformation.get(key));
    }

    @Override
    public String getQueryParam() {
        return queryInformation.get(QUERY_PARAM);
    }

    @Override
    public boolean isEmpty() {
        return tracks.isEmpty();
    }

}
