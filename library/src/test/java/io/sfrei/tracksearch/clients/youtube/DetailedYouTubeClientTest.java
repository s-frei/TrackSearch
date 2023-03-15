package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.ClientTest;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import org.junit.jupiter.api.Tag;

@Tag("DetailedClientTest")
public class DetailedYouTubeClientTest extends ClientTest<YouTubeTrack> {

    public DetailedYouTubeClientTest() {
        super(new YouTubeClient(), false);
    }

}
