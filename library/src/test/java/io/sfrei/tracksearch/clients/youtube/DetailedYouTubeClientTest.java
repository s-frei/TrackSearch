package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;

import static io.sfrei.tracksearch.clients.ClientTestConstants.SEARCH_KEYS;

@Slf4j
@Tag("DetailedClientTest")
public class DetailedYouTubeClientTest extends ClientTestImpl<YouTubeTrack> {

    public DetailedYouTubeClientTest() {
        super(new YouTubeClient(), SEARCH_KEYS, log);
    }

}
