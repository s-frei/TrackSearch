package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.YouTubeTrack;

public class YouTubeClientTest extends ClientTestImpl<YouTubeTrack> {

    public YouTubeClientTest() {
        super(new YouTubeClient());
    }

}
