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
import io.sfrei.tracksearch.clients.common.SharedClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public interface SearchClient<T extends Track> extends TrackSearchClient<T>, ClientLogger {

    default void throwIfPagingValueMissing(SearchClient<?> source, TrackList<? extends Track> trackList)
            throws TrackSearchException {

        if (!hasPagingValues(trackList))
            throw new TrackSearchException(String.format("Can not get next - paging value/s missing for %s", source.getClass().getSimpleName()));

    }

    default TrackSearchException noTrackStreamAfterRetriesException(Function<String, TrackSearchException> exceptionConstructor,
                                                                    int tries) {
        return exceptionConstructor.apply(String.format("Not able to get stream URL after %s tries", tries));
    }

    default TrackSearchException unsupportedQueryTypeException(Function<String, TrackSearchException> exceptionConstructor, QueryType queryType) {
        return exceptionConstructor.apply(String.format("Query type %s not supported", queryType));
    }

    default Optional<TrackStream> tryResolveTrackStream(T track, int retries) {
        log().trace("Get track stream for: {}", track);

        do {

            try {
                final TrackStream trackStream = getTrackStream(track);
                final Integer code = SharedClient.requestAndGetCode(trackStream.url());

                if (SharedClient.successResponseCode(code)) return Optional.of(trackStream);

                throw new TrackSearchException(String.format("Failed getting stream URL - response %s", code));

            } catch (TrackSearchException e) {
                retries--;
                if (retries > 0) {
                    log().warn("Not able getting stream for - {} tries left: {}", retries, e.getMessage());
                    try {
                        log().trace("Refreshing track information...");
                        refreshTrackInfo(track);
                    } catch (TrackSearchException ex) {
                        log().error("Failed refreshing track information", e);
                    }
                }
            }

        } while (retries >= 0);

        return Optional.empty();
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


    @Nullable
    default TrackStream trackStreamProvider(final T track) {
        try {
            return getTrackStream(track, TrackSearchConfig.resolvingRetries);
        } catch (TrackSearchException e) {
            log().error("Error occurred acquiring stream URL for: {}", track.getUrl(), e);
        }
        return null;
    }

}
