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

package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@UtilityClass
public class TrackListUtility {

    public <T extends Track> TrackList<T> updatePagingValues(final GenericTrackList<T> newTrackList,
                                                             final TrackList<? extends Track> oldTrackList,
                                                             final String positionKey, String offsetKey) {

        final String oldOffsetValue = oldTrackList.getQueryInformation().get(offsetKey);
        final String newOffsetValue = newTrackList.getQueryInformation().get(offsetKey);

        if (oldOffsetValue == null || newOffsetValue == null)
            return newTrackList.setPagingValues(positionKey, 0, offsetKey, 0);

        final int newPosition = Integer.parseInt(oldOffsetValue);
        final int offset = Integer.parseInt(newOffsetValue);
        final int newOffset = newPosition + offset;
        return newTrackList.setPagingValues(positionKey, newPosition, offsetKey, newOffset);
    }

    public void mergePositionValues(final GenericTrackList<? extends Track> trackList, final String positionKey, final String offsetKey) {
        final AtomicInteger position = new AtomicInteger(0);
        final AtomicInteger offset = new AtomicInteger(0);

        for (final String key : trackList.getQueryInformation().keySet()) {
            if (key.contains(TrackSearchConfig.POSITION_KEY_SUFFIX)) {
                position.getAndUpdate(pos -> pos += trackList.getQueryInformationIntValue(key));
            } else if (key.contains(TrackSearchConfig.OFFSET_KEY_SUFFIX)) {
                offset.getAndUpdate(off -> off += trackList.getQueryInformationIntValue(key));
            }
        }
        trackList.setPagingValues(positionKey, position.get(), offsetKey, offset.get());
    }

    public boolean hasQueryInformation(final TrackList<? extends Track> trackList, final String... keys) {
        final Map<String, String> queryInformation = trackList.getQueryInformation();
        for (final String key : keys) {
            if (queryInformation.get(key) == null)
                return false;
        }
        return true;
    }

}
