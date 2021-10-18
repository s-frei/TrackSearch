package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTestImpl<T extends Track> extends Client implements ClientTest {

    protected final TrackSearchClient<T> searchClient;

    private final List<String> searchKeys;

    private final List<TrackList<T>> tracksForSearch;

    protected final Logger logger;

    public ClientTestImpl(TrackSearchClient<T> trackSearchClient, List<String> searchKeys, Logger logger) {
        super(CookiePolicy.ACCEPT_ALL, null);
        searchClient = trackSearchClient;
        logger.debug("Initialized test suite for: {}", searchClient.getClass().getSimpleName());
        this.searchKeys = searchKeys;
        this.logger = logger;
        tracksForSearch = new ArrayList<>();
    }

    @Override
    @Order(1)
    @Test
    public void tracksFoSearch() throws TrackSearchException {
        logger.info("Search for {}", Arrays.toString(searchKeys.toArray()));
        final TestLogger testLogger = TestLogger.withLogger(logger);
        for (String key : searchKeys) {
            testLogger.logBefore();

            logger.info("Search for: {}", key);
            TrackList<T> tracksForSearch = searchClient.getTracksForSearch(key);
            logger.info("Found tracks: {}", tracksForSearch.getTracks().size());
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
        final TestLogger testLogger = TestLogger.withLogger(logger);
        for (TrackList<T> trackList : tracksForSearch) {
            testLogger.logBefore();

            logger.info("Next for: {}", trackList.getQueryParam());
            TrackList<T> nextTracksForSearch = searchClient.getNext(trackList);
            logger.info("Found tracks: {}", nextTracksForSearch.getTracks().size());
            assertFalse(nextTracksForSearch.isEmpty());
        }
    }

    @Override
    @Order(3)
    @Test
    public void checkMetadata() {
        final TestLogger testLogger = TestLogger.withLogger(logger);
        for (TrackList<T> trackList : tracksForSearch) {
            for (T track : trackList.getTracks()) {
                testLogger.logBefore();

                final TrackMetadata trackMetadata = track.getTrackMetadata();
                final String metaDataString = trackMetadata.toString();

                logger.info("MetaData for: {}", track);
                if (trackMetadata.getChannelName() == null ||
                        trackMetadata.getChannelUrl() == null ||
                        trackMetadata.getStreamAmount() == null ||
                        trackMetadata.getThumbNailUrl() == null)
                    logger.warn("Any MetaData unresolved: {}", metaDataString);

                logger.debug("MetaData: {}", metaDataString);
            }
        }
    }

    @Override
    @Order(4)
    @Test
    public void getStreamUrl() {
        final TestLogger testLogger = TestLogger.withLogger(logger);
        for (TrackList<T> trackList : tracksForSearch) {
            for (T track : trackList.getTracks()) {
                testLogger.logBefore();

                logger.info("Trying to get stream url for: {}", track.toString());
                String streamUrl = track.getStreamUrl();
                assertNotNull(streamUrl);
                logger.info("URL found: {}", streamUrl);
                final int code = requestAndGetCode(streamUrl);
                logger.debug("Response code: {}", code);
                assertNotEquals(FORBIDDEN, code);
            }
        }
    }

    /**
     * Improve readability, especially in GitHub actions
     */
    @RequiredArgsConstructor(staticName = "withLogger")
    private static class TestLogger {

        private final Logger log;

        private final AtomicInteger pos = new AtomicInteger(1);

        protected void logBefore() {
            log.info("======= {} =======", String.format("%03d", pos.getAndIncrement()));
        }

    }

}
