package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import org.junit.jupiter.api.Tag;

import java.util.Collections;

import static io.sfrei.tracksearch.clients.ClientTestConstants.DEFAULT_SEARCH_KEY;
@Tag("ClientTest")
public class YouTubeClientTest extends ClientTestImpl<YouTubeTrack> {

    public YouTubeClientTest() {
        super(new YouTubeClient(), Collections.singletonList(DEFAULT_SEARCH_KEY));
    }

}
