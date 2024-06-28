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

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.NonNull;

import java.util.Set;

/**
 * Main interface containing all functionality a client offers to the user.
 *
 * @param <T> the track type the client implementing this is used for.
 */
public interface TrackSearchClient<T extends Track> {

    /**
     * Retrieve all valid URL prefixes used to check {@link #isApplicableForURL(String)}.
     *
     * @return the set of valid URL prefixes.
     */
    Set<String> validURLPrefixes();

    /**
     * Test if this client can handle the provided URL to make sure it can
     * be used for {@link #getTrack(String)}.
     *
     * @param url the URL to check.
     * @return true if this client is applicable and false if not.
     */
    default boolean isApplicableForURL(@NonNull String url) {
        return validURLPrefixes().stream().anyMatch(url::startsWith);
    }

    /**
     * Get a track for the given URL.
     *
     * @param url the URL to create track for.
     * @return the track for the provided URL.
     * @throws TrackSearchException when the track cannot ba created.
     */
    T getTrack(@NonNull String url) throws TrackSearchException;

    /**
     * Search for tracks using a string containing keywords.
     *
     * @param search keywords to search for.
     * @return a track list containing all found tracks.
     * @throws TrackSearchException when the client encountered a problem on searching.
     */
    TrackList<T> getTracksForSearch(@NonNull String search) throws TrackSearchException;

    /**
     * Search for the next tracks for last result.
     *
     * @param trackList a previous search result for that client.
     * @return a track list containing the next tracks available.
     * @throws TrackSearchException when the client encounters a problem on getting the next tracks.
     */
    TrackList<T> getNext(@NonNull TrackList<? extends Track> trackList) throws TrackSearchException;

    /**
     * Check the track list for this client if the paging values to get next are present.
     *
     * @param trackList a previous search result for this client.
     * @return either the paging values are present or not.
     */
    boolean hasPagingValues(@NonNull TrackList<? extends Track> trackList);

}
