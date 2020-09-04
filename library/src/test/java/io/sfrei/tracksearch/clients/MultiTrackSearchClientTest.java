package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.sfrei.tracksearch.clients.ClientTestConstants.SEARCH_KEY;

/**
 * Even tho the clients are already tested separately
 * test the async "multi" client
 */
@Slf4j
@Tag("ClientTest")
public class MultiTrackSearchClientTest extends ClientTestImpl<Track> {


    public MultiTrackSearchClientTest() {
        super(new MultiSearchClient(), Collections.singletonList(SEARCH_KEY));
    }

    @Test
    public void testYTSource() throws TrackSearchException {
        MultiTrackSearchClient searchClient = (MultiTrackSearchClient) this.searchClient;
        TrackSource trackSource = TrackSource.Youtube;
        log.debug("MultiTrackSearchClient with explicit source ->  {}", trackSource);
        searchClient.getTracksForSearch(SEARCH_KEY, TrackSource.setOf(trackSource));
    }

    @Test
    public void testSCSource() throws TrackSearchException {
        MultiTrackSearchClient searchClient = (MultiTrackSearchClient) this.searchClient;
        TrackSource trackSource = TrackSource.Soundcloud;
        log.debug("MultiTrackSearchClient with explicit source ->  {}", trackSource);
        searchClient.getTracksForSearch(SEARCH_KEY, TrackSource.setOf(trackSource));
    }

}
