/*
 * Copyright (C) 2023 s-frei (sfrei.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.sfrei.tracksearch.clients.setup.TrackSource.Soundcloud;
import static io.sfrei.tracksearch.clients.setup.TrackSource.Youtube;

/**
 * Even tho the clients are already tested separately
 * test the async "multi" client
 */
@Slf4j
@Tag("ClientTest")
public class MultiTrackSearchClientTest extends ClientTest<MultiTrackSearchClient, Track> {

    public MultiTrackSearchClientTest() {
        super(new MultiSearchClient(), true);
    }

    @Test
    public void testYTSource() throws TrackSearchException {
        testSource(Youtube);
    }

    @Test
    public void testSCSource() throws TrackSearchException {
        testSource(Soundcloud);
    }

    private void testSource(TrackSource source) throws TrackSearchException {
        log.debug("MultiTrackSearchClient with explicit source ->  {}", source);
        final TrackList<Track> trackList = searchClient.getTracksForSearch(SINGLE_SEARCH_KEY, TrackSource.setOf(source));
        log.debug("Found '{}' tracks for {}", trackList.size(), source);
    }

}
