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

package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import org.jetbrains.annotations.Nullable;

import java.net.CookiePolicy;
import java.util.Map;

public abstract class SingleSearchClient<T extends Track> extends Client implements TrackSearchClient<T> {

    public SingleSearchClient(@Nullable CookiePolicy cookiePolicy, @Nullable Map<String, String> headers) {
        super(cookiePolicy, headers);
    }

    protected void throwIfPagingValueMissing(SingleSearchClient<?> source, TrackList<? extends Track> trackList)
            throws TrackSearchException {

        if (!hasPagingValues(trackList))
            throw new TrackSearchException(String.format("Can not get next - paging value/s missing for %s", source.getClass().getSimpleName()));

    }

}
