package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.ClientTest;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import org.junit.jupiter.api.Tag;

@Tag("ClientTest")
public class YouTubeClientTest extends ClientTest<YouTubeTrack> {

    public YouTubeClientTest() {
        super(new YouTubeClient(), true);
    }

}
