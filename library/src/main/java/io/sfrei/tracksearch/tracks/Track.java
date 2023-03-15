package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import io.sfrei.tracksearch.utils.ReplaceUtility;
import io.sfrei.tracksearch.utils.TimeUtility;

import java.time.Duration;

public interface Track {

    /**
     * Identify where the tracks came from.
     * @return the source the track was received from.
     */
    TrackSource getSource();

    /**
     * Get the track title.
     * @return the track title.
     */
    String getTitle();

    /**
     * Get the track title without unnecessary stuff.
     * @return the clean track title.
     */
    default String getCleanTitle() {
        return ReplaceUtility.cleanOutTitle(getTitle());
    }

    /**
     * Get the track duration.
     * @return the track duration.
     */
    Duration getDuration();

    /**
     * Get the track duration formatted like "hh:mm:ss" when
     * hours present, else like "mm:ss".
     * @return the formatted duration.
     */
    default String durationFormatted() {
        return TimeUtility.formatSeconds(getDuration());
    }

    /**
     * Get the URL for the real content.
     * @return the real content URL.
     */
    String getUrl();

    /**
     * Get the audio stream URL in the highest possible quality. The resulting URL will be
     * checked if it can be successfully accessed, if under some circumstances this fails,
     * the resolver will start another attempt - once.
     *
     * @return the audio stream URL, null when exception occurred.
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
     * @return the pretty string.
     */
    String pretty();

    /**
     * Get a pretty string representation with a clean title.
     * @return the pretty string with clean title.
     */
    String prettyAndClean();

}
