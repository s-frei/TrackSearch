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

package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import io.sfrei.tracksearch.utils.ReplaceUtility;
import io.sfrei.tracksearch.utils.TimeUtility;

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
        return ReplaceUtility.cleanOutTitle(getTitle());
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
        return TimeUtility.formatSeconds(getDuration());
    }

    /**
     * Get the URL for the real content.
     *
     * @return the real content URL.
     */
    String getUrl();

    /**
     * Get the audio stream URL in the highest possible quality. The resulting URL will be
     * checked if it can be successfully accessed, if under some circumstances this fails,
     * the resolver will start some more attempts.
     * To override default {@link io.sfrei.tracksearch.config.TrackSearchConfig#resolvingRetries}
     *
     * @return the audio stream URL or null when resolving fails.
     */
    String getStreamUrl();

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
