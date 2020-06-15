package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.TrackSource;

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
    String getCleanTitle();

    /**
     * Get the track length in seconds.
     * @return the track length.
     */
    Long getLength();

    /**
     * Get the URL for the real content.
     * @return the real content URL.
     */
    String getUrl();

    /**
     * Get the audio stream URL in the highest possible quality.
     * @return the audio stream URL.
     */
    String getStreamUrl();

    /**
     * Check if this track equals another using the URL.
     * @param o the other track object.
     * @return if this track equals another.
     */
    boolean equals(Object o);

    /**
     * Get a pretty string representation.
     * @return the pretty string.
     */
    String toPrettyString();

    /**
     * Get a pretty string representation with a clean title.
     * @return the pretty string with clean title.
     */
    String toPrettyCleanString();

}
