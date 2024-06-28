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

package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.common.ClientLogger;
import io.sfrei.tracksearch.clients.common.QueryType;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface SearchClient<T extends Track> extends TrackSearchClient<T>, ClientLogger {

    default void throwIfPagingValueMissing(SearchClient<? extends Track> source, TrackList<? extends Track> trackList)
            throws TrackSearchException {

        if (!hasPagingValues(trackList))
            throw new TrackSearchException(String.format("Can not get next - paging value/s missing for %s", source.getClass().getSimpleName()));

    }

    default TrackSearchException unsupportedQueryTypeException(Function<String, TrackSearchException> exceptionConstructor, QueryType queryType) {
        return exceptionConstructor.apply(String.format("Query type %s not supported", queryType));
    }


    @Nullable
    default TrackList<T> provideNext(final TrackList<T> trackList) {
        try {
            return getNext(trackList);
        } catch (TrackSearchException e) {
            log().error("Error occurred acquiring next track list", e);
        }
        return null;
    }

}
