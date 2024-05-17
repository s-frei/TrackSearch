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

package io.sfrei.tracksearch.clients.interfaces.functional;

import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@FunctionalInterface
public interface TrackStreamProvider<T extends Track> extends Function<T, TrackStream> {

    /**
     * Function to return the stream URL for given track.
     * @param t the track to get the stream URL for.
     * @return the stream URL or null when any exception occurred.
     */
    @Nullable
    @Override
    TrackStream apply(T t);

}
