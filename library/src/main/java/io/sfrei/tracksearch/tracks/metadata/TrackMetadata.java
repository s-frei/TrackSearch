package io.sfrei.tracksearch.tracks.metadata;

public interface TrackMetadata {

    /**
     * Get the name of the publishers channel.
     *
     * @return the channel name.
     */
    String getChannelName();

    /**
     * Get the URL of the publishers channel.
     *
     * @return the channel URL.
     */
    String getChannelUrl();

    /**
     * Get the amount of streams.
     *
     * @return the stream amount.
     */
    Long getStreamAmount();

    /**
     * Get the URL of the media thumbnail (small).
     *
     * @return the thumbnail URL.
     */
    String getThumbNailUrl();

}
