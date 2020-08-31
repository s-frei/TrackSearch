package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * Even tho the clients are already tested separately
 * test the async "multi" client
 */
@Slf4j
public class MultiTrackSearchClientTest extends ClientTestImpl<Track> {


    public MultiTrackSearchClientTest() {
        super(new MultiSearchClient());
    }

    @Test
    public void testYTSource() throws TrackSearchException {
        MultiTrackSearchClient searchClient = (MultiTrackSearchClient) this.searchClient;
        TrackSource trackSource = TrackSource.Youtube;
        log.debug("MultiTrackSearchClient with explicit source ->  {}", trackSource);
        searchClient.getTracksForSearch(ClientTestConstants.SEARCH_KEYS, TrackSource.setOf(trackSource));
    }

    @Test
    public void testSCSource() throws TrackSearchException {
        MultiTrackSearchClient searchClient = (MultiTrackSearchClient) this.searchClient;
        TrackSource trackSource = TrackSource.Soundcloud;
        log.debug("MultiTrackSearchClient with explicit source ->  {}", trackSource);
        searchClient.getTracksForSearch(ClientTestConstants.SEARCH_KEYS, TrackSource.setOf(trackSource));
    }

}
