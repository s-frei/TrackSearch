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

package io.sfrei.tracksearch.clients.common;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public interface SearchClient<T extends Track> extends TrackSearchClient<T>, ClientLogger {
    int INITIAL_TRY = 1;

    default void throwIfPagingValueMissing(SearchClient<?> source, TrackList<? extends Track> trackList)
            throws TrackSearchException {

        if (!hasPagingValues(trackList))
            throw new TrackSearchException(String.format("Can not get next - paging value/s missing for %s", source.getClass().getSimpleName()));

    }

    /**
     * Exception with uniform message for failed stream URL resolving after several retries.
     *
     * @param exceptionConstructor the constructor for the exception to create.
     * @param retries              the retries which were taken.
     * @return the exception.
     */
    default TrackSearchException noTrackStreamAfterRetriesException(Function<String, TrackSearchException> exceptionConstructor,
                                                                    int retries) {
        return exceptionConstructor.apply(String.format("Not able to get stream URL after %s tries", retries + INITIAL_TRY));
    }

    /**
     * Exception with uniform message when query type is not supported for some circumstances.
     *
     * @param exceptionConstructor the constructor for the exception to create.
     * @param queryType            the unsupported query type.
     * @return the exception.
     */
    default TrackSearchException unsupportedQueryTypeException(Function<String, TrackSearchException> exceptionConstructor, QueryType queryType) {
        return exceptionConstructor.apply(String.format("Query type %s not supported", queryType));
    }

    default Optional<TrackStream> getTrackStream(TrackSearchClient<T> searchClient, T track,
                                                 final int retries) {
        log().trace("Get stream URL for: {}", track);
        return tryGetTrackStream(searchClient, track, retries + INITIAL_TRY);
    }

    private Optional<TrackStream> tryGetTrackStream(TrackSearchClient<T> searchClient, T track,
                                                    int tries) {

        if (tries <= 0)
            return Optional.empty();

        try {
            final TrackStream trackStream = searchClient.getTrackStream(track);
            final Integer code = SharedClient.requestAndGetCode(trackStream.url());
            if (SharedClient.successResponseCode(code))
                return Optional.of(trackStream);
            else {
                tries--;
                if (tries > 0) log().warn("Not able getting stream URL for {} - {} retries left",
                        searchClient.getClass().getSimpleName(), tries);

                return tryGetTrackStream(searchClient, track, tries);
            }
        } catch (TrackSearchException e) {
            log().error("Error getting stream URL using {}", searchClient.getClass().getSimpleName(), e);
            return Optional.empty();
        }

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
            log().error("Error occurred acquiring stream URL", e);
        }
        return null;
    }

}
