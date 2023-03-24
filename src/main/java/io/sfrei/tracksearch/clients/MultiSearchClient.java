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

import io.sfrei.tracksearch.clients.interfaces.Provider;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.clients.soundcloud.SoundCloudClient;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.*;
import io.sfrei.tracksearch.utils.TrackListUtility;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings({"FieldCanBeLocal", "unchecked"})
public class MultiSearchClient implements MultiTrackSearchClient, Provider<Track> {

    public static final String POSITION_KEY = "multi" + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = "multi" + TrackSearchConfig.OFFSET_KEY_SUFFIX;

    private final TrackSearchClient<YouTubeTrack> youTubeClient;
    private final TrackSearchClient<SoundCloudTrack> soundCloudClient;

    private final Map<TrackSource, TrackSearchClient<? extends Track>> clientsForSource = new HashMap<>();

    public MultiSearchClient() {
        this.youTubeClient = new YouTubeClient();
        this.soundCloudClient = new SoundCloudClient();

        clientsForSource.put(TrackSource.Youtube, youTubeClient);
        clientsForSource.put(TrackSource.Soundcloud, soundCloudClient);

        log.info("TrackSearchClient created with {} clients", allClients().size());
    }

    private List<TrackSearchClient<? extends Track>> allClients() {
        return new ArrayList<>(clientsForSource.values());
    }

    @Override
    public TrackList<Track> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        return getTracksForSearch(search, allClients());
    }

    @Override
    public TrackList<Track> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {

        final List<TrackSearchClient<? extends Track>> callClient = allClients().stream()
                .filter(client -> client.hasPagingValues(trackList))
                .collect(Collectors.toList());

        return getNext(trackList, callClient);
    }

    @Override
    public String getStreamUrl(@NonNull final Track track) throws TrackSearchException {

        if (track instanceof YouTubeTrack) {
            return youTubeClient.getStreamUrl((YouTubeTrack) track);
        } else if (track instanceof SoundCloudTrack) {
            return soundCloudClient.getStreamUrl((SoundCloudTrack) track);
        }
        throw new TrackSearchException("Track type is unknown");
    }

    @Override
    public String getStreamUrl(@NonNull Track track, int retries) throws TrackSearchException {
        if (track instanceof YouTubeTrack) {
            return youTubeClient.getStreamUrl((YouTubeTrack) track, retries);
        } else if (track instanceof SoundCloudTrack) {
            return soundCloudClient.getStreamUrl((SoundCloudTrack) track, retries);
        }
        throw new TrackSearchException("Track type is unknown");
    }

    @Override
    public TrackList<Track> getTracksForSearch(@NonNull final String search, @NonNull final Set<TrackSource> sources)
            throws TrackSearchException {

        if (sources.isEmpty())
            throw new TrackSearchException("Provide at least one source");

        final List<TrackSearchClient<? extends Track>> callClients = sources.stream()
                .filter(clientsForSource::containsKey)
                .map(clientsForSource::get)
                .collect(Collectors.toList());

        return getTracksForSearch(search, callClients);
    }

    private GenericTrackList<Track> getTracksForSearch(final String search, final List<TrackSearchClient<? extends Track>> callClients)
            throws TrackSearchException {
        final List<Callable<GenericTrackList<Track>>> searchCalls = new ArrayList<>();

        for (final TrackSearchClient<? extends Track> client : callClients) {
            searchCalls.add(() -> (GenericTrackList<Track>) client.getTracksForSearch(search));
        }

        log.debug("Performing search call for {} clients", callClients.size());
        return getMergedTrackListFromCalls(searchCalls, QueryType.SEARCH);
    }

    private TrackList<Track> getNext(final TrackList<? extends Track> trackList, final List<TrackSearchClient<? extends Track>> callClients)
            throws TrackSearchException {

        final List<Callable<GenericTrackList<Track>>> nextCalls = new ArrayList<>();

        for (final TrackSearchClient<? extends Track> client : callClients) {
            nextCalls.add(() -> (GenericTrackList<Track>) client.getNext(trackList));
        }
        log.debug("Performing next call for {} clients", callClients.size());
        return getMergedTrackListFromCalls(nextCalls, trackList.getQueryType());
    }

    private GenericTrackList<Track> getMergedTrackListFromCalls(final List<Callable<GenericTrackList<Track>>> calls, final QueryType queryType)
            throws TrackSearchException {

        final ExecutorService executorService = Executors.newFixedThreadPool(calls.size());

        final GenericTrackList<Track> list = GenericTrackList.builder()
                .queryType(queryType)
                .nextTrackListFunction(this::provideNext)
                .build();

        try {
            for (final Future<GenericTrackList<Track>> trackList : executorService.invokeAll(calls)) {
                list.mergeIn(trackList.get());
            }
        } catch (InterruptedException e) {
            throw new TrackSearchException(e);
        } catch (ExecutionException e) {
            throw new TrackSearchException("An error occurred acquiring a tracklist", e);
        }

        TrackListUtility.mergePositionValues(list, POSITION_KEY, OFFSET_KEY);

        return list;
    }

    @Override
    public boolean hasPagingValues(@NotNull final TrackList<? extends Track> trackList) {
        return TrackListUtility.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY);
    }

    @Override
    public Logger log() {
        return log;
    }

}
