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

import io.sfrei.tracksearch.clients.common.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Interact with all available (all implemented) clients at the same time. All calls will get
 * processed asynchronous.
 */
public interface MultiTrackSearchClient extends TrackSearchClient<Track> {

    /**
     * Search for tracks using a string containing keywords on given track sources.
     *
     * @param search  keywords to search for.
     * @param sources to search on.
     * @return a track list containing all found tracks for selected clients.
     * @throws TrackSearchException when any client encountered a problem while searching.
     */
    TrackList<Track> getTracksForSearch(@NotNull String search, Set<TrackSource> sources) throws TrackSearchException;

}
