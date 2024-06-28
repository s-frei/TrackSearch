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

package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import io.sfrei.tracksearch.utils.DurationParser;
import io.sfrei.tracksearch.utils.StringReplacer;

import java.time.Duration;

public interface Track {

    /**
     * Identify where the tracks came from.
     *
     * @return the source the track was received from.
     */
    TrackSource getSource();

    /**
     * Get the track title.
     *
     * @return the track title.
     */
    String getTitle();

    /**
     * Get the track title without unnecessary stuff.
     *
     * @return the clean track title.
     */
    default String getCleanTitle() {
        return StringReplacer.cleanTitle(getTitle());
    }

    /**
     * Get the track duration.
     *
     * @return the track duration.
     */
    Duration getDuration();

    /**
     * Get the track duration formatted like "hh:mm:ss" when
     * hours present, else like "mm:ss".
     *
     * @return the formatted duration.
     */
    default String durationFormatted() {
        return DurationParser.formatSeconds(getDuration());
    }

    /**
     * Get the URL for the real content.
     *
     * @return the real content URL.
     */
    String getUrl();

    /**
     * Get metadata like, channel, views and so on.
     *
     * @return the object containing the track metadata.
     */
    TrackMetadata getTrackMetadata();

    /**
     * Check if this track equals another using the URL.
     *
     * @param o the other track object.
     * @return if this track equals another.
     */
    boolean equals(Object o);

    /**
     * Get a pretty string representation.
     *
     * @return the pretty string.
     */
    String pretty();

    /**
     * Get a pretty string representation with a clean title.
     *
     * @return the pretty string with clean title.
     */
    String prettyAndClean();

}
