package io.sfrei.tracksearch.tracks.metadata;

import lombok.ToString;
import lombok.Value;

@Value
@ToString
public class YouTubeTrackMetadata implements TrackMetadata {

    String channelName;

    String channelUrl;

    Long streamAmount;

    String thumbNailUrl;

}
