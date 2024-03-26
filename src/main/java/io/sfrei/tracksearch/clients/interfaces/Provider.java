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

package io.sfrei.tracksearch.clients.interfaces;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import org.jetbrains.annotations.Nullable;

public interface Provider<T extends Track> extends TrackSearchClient<T>, ClassLogger {

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
    default String streamURLProvider(final T track) {
        try {
            return getStreamUrl(track, TrackSearchConfig.resolvingRetries);
        } catch (TrackSearchException e) {
            log().error("Error occurred acquiring stream URL", e);
        }
        return null;
    }

}
