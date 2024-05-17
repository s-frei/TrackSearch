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

package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.QueryType;

import java.util.List;
import java.util.Map;

public interface TrackList<T extends Track> extends List<T> {

    String QUERY_KEY = "query";

    /**
     * The type of query tracks were returned for.
     * @return the used query type.
     */
    QueryType getQueryType();

    /**
     * Return the next track list for query.
     * @return the next track list.
     */
    TrackList<T> next();

    /**
     * Get all information used for the query.
     * @return all query information.
     */
    Map<String, String> getQueryInformation();


    /**
     * Get a query information parsed to an int.
     * @param key the key of the query information.
     * @return the int value of the query information.
     */
    Integer queryInformationAsInt(String key);

    /**
     * Add a query information value.
     * @param key the key of the query information.
     * @param value the value of the query information.
     */
    void addQueryInformationValue(String key, int value);

    /**
     * Get the value used for the query.
     * @return the query value.
     */
    default String getQueryValue() {
        return getQueryInformation().get(QUERY_KEY);
    }

}
