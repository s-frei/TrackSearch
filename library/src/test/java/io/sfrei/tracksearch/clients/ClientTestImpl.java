package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTestImpl<T extends Track> implements ClientTest {

    protected final TrackSearchClient<T> searchClient;

    private TrackList<T> tracksForSearch;

    public ClientTestImpl(TrackSearchClient<T> trackSearchClient) {
        searchClient = trackSearchClient;
    }

    @Override
    @Order(1)
    @Test
    public void tracksFoSearch() throws TrackSearchException {
        log.debug("Search for {}", ClientTestConstants.SEARCH_KEYS);
        tracksForSearch = searchClient.getTracksForSearch(ClientTestConstants.SEARCH_KEYS);
        log.debug("Found tracks: {}", tracksForSearch.getTracks().size());
        assertFalse(tracksForSearch.isEmpty());
    }

    @Override
    @Order(2)
    @Test
    public void trackListGotPagingValues() {
        assertTrue(searchClient.hasPagingValues(tracksForSearch));
    }

    @Override
    @Order(3)
    @Test
    public void getNextTracks() throws TrackSearchException {
        TrackList<T> nextTracksForSearch = searchClient.getNext(tracksForSearch);
        log.debug("Found tracks: {}", nextTracksForSearch.getTracks().size());
        assertFalse(nextTracksForSearch.isEmpty());
    }

    @Override
    @Order(4)
    @Test
    public void getStreamUrl() {
        tracksForSearch.getTracks().forEach(track -> {
            log.info("Trying to get stream url for: {}", track.toString());
            String streamUrl = track.getStreamUrl();
            assertNotNull(streamUrl);
            log.info("URL found: {}", streamUrl);
        });
    }
}
