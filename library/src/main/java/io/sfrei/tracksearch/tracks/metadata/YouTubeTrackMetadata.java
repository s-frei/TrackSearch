package io.sfrei.tracksearch.tracks.metadata;

import lombok.Value;

@Value
public class YouTubeTrackMetadata implements TrackMetadata {

    String channelName;

    String channelUrl;

    Long streamAmount;

}
