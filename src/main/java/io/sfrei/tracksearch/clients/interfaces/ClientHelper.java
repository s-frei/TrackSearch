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

package io.sfrei.tracksearch.clients.interfaces;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;

import java.util.Optional;
import java.util.function.Function;


public interface ClientHelper extends ClientLogger {

    int INITIAL_TRY = 1;

    default <T extends Track> Optional<TrackStream> getTrackStream(TrackSearchClient<T> searchClient, T track,
                                                                   Function<String, Integer> requestForCodeFunction,
                                                                   final int retries) {
        log().trace("Get stream URL for: {}", track);
        return tryGetTrackStream(searchClient, track, requestForCodeFunction, retries + INITIAL_TRY);
    }

    private <T extends Track> Optional<TrackStream> tryGetTrackStream(TrackSearchClient<T> searchClient, T track,
                                                                      Function<String, Integer> requestForCodeFunction,
                                                                      int tries) {

        if (tries <= 0)
            return Optional.empty();

        try {
            final TrackStream trackStream = searchClient.getTrackStream(track);
            final Integer code = requestForCodeFunction.apply(trackStream.url());
            if (Client.successResponseCode(code))
                return Optional.of(trackStream);
            else {
                tries--;
                if (tries > 0) log().warn("Not able getting stream URL for {} - {} retries left",
                        searchClient.getClass().getSimpleName(), tries);

                return tryGetTrackStream(searchClient, track, requestForCodeFunction, tries);
            }
        } catch (TrackSearchException e) {
            log().error("Error getting stream URL using {}", searchClient.getClass().getSimpleName(), e);
            return Optional.empty();
        }

    }

}
