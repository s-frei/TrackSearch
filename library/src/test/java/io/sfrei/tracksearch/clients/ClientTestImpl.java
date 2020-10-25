package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTestImpl<T extends Track> extends Client implements ClientTest {

    protected final TrackSearchClient<T> searchClient;

    private final List<String> searchKeys;

    private final List<TrackList<T>> tracksForSearch;

    public ClientTestImpl(TrackSearchClient<T> trackSearchClient, List<String> searchKeys) {
        searchClient = trackSearchClient;
        log.debug("Initialized {}", searchClient.getClass().getSimpleName());
        this.searchKeys = searchKeys;
        tracksForSearch = new ArrayList<>();
    }

    @Override
    @Order(1)
    @Test
    public void tracksFoSearch() throws TrackSearchException {
        log.info("Search for {}", Arrays.toString(searchKeys.toArray()));
        for (String key : searchKeys) {
            log.info("Search for: {}", key);
            TrackList<T> tracksForSearch = searchClient.getTracksForSearch(key);
            log.info("Found tracks: {}", tracksForSearch.getTracks().size());
            assertFalse(tracksForSearch.isEmpty());
            this.tracksForSearch.add(tracksForSearch);
        }
    }

    @Override
    @Order(2)
    @Test
    public void trackListGotPagingValues() {
        tracksForSearch.forEach(trackList -> assertTrue(searchClient.hasPagingValues(trackList)));
    }

    @Override
    @Order(3)
    @Test
    public void getNextTracks() throws TrackSearchException {
        for (TrackList<T> trackList : tracksForSearch) {
            log.info("Next for: {}", trackList.getQueryParam());
            TrackList<T> nextTracksForSearch = searchClient.getNext(trackList);
            log.info("Found tracks: {}", nextTracksForSearch.getTracks().size());
            assertFalse(nextTracksForSearch.isEmpty());
        }
    }

    @Override
    @Order(4)
    @Test
    public void getStreamUrl() throws IOException {
        AtomicInteger pos = new AtomicInteger(0);
        for (TrackList<T> trackList : tracksForSearch) {
            for (T track : trackList.getTracks()) {
                log.info("{} - Trying to get stream url for: {}", pos.getAndIncrement(), track.toString());
                String streamUrl = track.getStreamUrl();
                assertNotNull(streamUrl);
                log.info("URL found: {}", streamUrl);
                final int code = requestAndGetCode(streamUrl);
                log.debug("Response received - code: {}", code);
                assertEquals(OK, code);
            }
        }
    }
}
