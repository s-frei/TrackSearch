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
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import org.jetbrains.annotations.Nullable;

public interface TrackProviders<T extends Track> extends TrackSearchClient<T>, ClientLogger {


    <I> I getTrackInfo(T track) throws TrackSearchException;

    @Nullable
    default <I> I trackInfoProvider(final T track) {
        try {
            return getTrackInfo(track);
        } catch (TrackSearchException e) {
            log().error("Error occurred acquiring track info for: {}", track.getUrl(), e);
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
