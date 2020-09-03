package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTestImpl<T extends Track> implements ClientTest {

    protected final TrackSearchClient<T> searchClient;

    private final List<String> searchKeys;

    private final List<TrackList<T>> tracksForSearch;

    public ClientTestImpl(TrackSearchClient<T> trackSearchClient, List<String> searchKeys) {
        searchClient = trackSearchClient;
        this.searchKeys = searchKeys;
        tracksForSearch = new ArrayList<>();
    }

    @Override
    @Order(1)
    @Test
    public void tracksFoSearch() throws TrackSearchException {
        log.debug("Search for {}", Arrays.toString(searchKeys.toArray()));
        for (String key : searchKeys) {
            TrackList<T> tracksForSearch = searchClient.getTracksForSearch(key);
            log.debug("Found tracks: {}", tracksForSearch.getTracks().size());
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
            TrackList<T> nextTracksForSearch = searchClient.getNext(trackList);
            log.debug("Found tracks: {}", nextTracksForSearch.getTracks().size());
            assertFalse(nextTracksForSearch.isEmpty());
        }
    }

    @Override
    @Order(4)
    @Test
    public void getStreamUrl() {
        tracksForSearch.forEach(trackList -> trackList.getTracks().forEach(track -> {
            log.info("Trying to get stream url for: {}", track.toString());
            String streamUrl = track.getStreamUrl();
            assertNotNull(streamUrl);
            log.info("URL found: {}", streamUrl);
        }));
    }
}
