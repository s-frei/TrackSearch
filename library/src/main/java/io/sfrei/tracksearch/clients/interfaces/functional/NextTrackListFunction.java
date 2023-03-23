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

package io.sfrei.tracksearch.clients.interfaces.functional;

import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@FunctionalInterface
public interface NextTrackListFunction<T extends Track> extends Function<TrackList<T>, TrackList<T>> {

    /**
     * Function to return the next tracks for given tracklist.
     * @param trackList the tracklist holding the paging information to get next for.
     * @return the next tracklist or null when any exception occurred.
     */
    @Nullable
    @Override
    TrackList<T> apply(TrackList<T> trackList);

}
