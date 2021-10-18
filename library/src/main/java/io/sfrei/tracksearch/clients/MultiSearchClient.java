package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.clients.soundcloud.SoundCloudClient;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.*;
import io.sfrei.tracksearch.utils.TrackListHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@SuppressWarnings({"FieldCanBeLocal", "unchecked"})
public class MultiSearchClient implements MultiTrackSearchClient {

    public static final String POSITION_KEY = "multi" + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = "multi" +  TrackSearchConfig.OFFSET_KEY_SUFFIX;

    private final TrackSearchClient<YouTubeTrack> youTubeClient;
    private final TrackSearchClient<SoundCloudTrack> soundCloudClient;

    private final List<TrackSearchClient<? extends Track>> clients = new ArrayList<>();

    public MultiSearchClient() {
        this.youTubeClient = new YouTubeClient();
        this.soundCloudClient = new SoundCloudClient();

        clients.add(youTubeClient);
        clients.add(soundCloudClient);
        log.info("TrackSearchClient created with {} clients", clients.size());
    }

    @Override
    public TrackList<Track> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        return getTracksForSearch(search, clients);
    }

    @Override
    public TrackList<Track> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {

        final List<TrackSearchClient<? extends Track>> callClients = new ArrayList<>(clients);

        if (!youTubeClient.hasPagingValues(trackList)) {
            callClients.remove(youTubeClient);
        }
        if (!soundCloudClient.hasPagingValues(trackList)) {
            callClients.remove(soundCloudClient);
        }
        return getNext(trackList, callClients);
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

        final List<TrackSearchClient<? extends Track>> callClients = new ArrayList<>(clients);

        if (!sources.contains(TrackSource.Youtube)) {
            callClients.remove(youTubeClient);
        }
        if (!sources.contains(TrackSource.Soundcloud)) {
            callClients.remove(soundCloudClient);
        }
        return getTracksForSearch(search, callClients);
    }

    private BaseTrackList<Track> getTracksForSearch(final String search, final List<TrackSearchClient<? extends Track>> callClients)
            throws TrackSearchException {
        final List<Callable<BaseTrackList<Track>>> searchCalls = new ArrayList<>();

        for (final TrackSearchClient<? extends Track> client : callClients) {
            searchCalls.add(() -> (BaseTrackList<Track>) client.getTracksForSearch(search));
        }

        log.debug("Performing search call for {} clients", callClients.size());
        return getMergedTrackListFromCalls(searchCalls, QueryType.SEARCH);
    }

    private TrackList<Track> getNext(final TrackList<? extends Track> trackList, final List<TrackSearchClient<? extends Track>> callClients)
            throws TrackSearchException {

        final List<Callable<BaseTrackList<Track>>> nextCalls = new ArrayList<>();

        for (final TrackSearchClient<? extends Track> client : callClients) {
            nextCalls.add(() -> (BaseTrackList<Track>) client.getNext(trackList));
        }
        log.debug("Performing next call for {} clients", callClients.size());
        return getMergedTrackListFromCalls(nextCalls, trackList.getQueryType());
    }

    private BaseTrackList<Track> getMergedTrackListFromCalls(final List<Callable<BaseTrackList<Track>>> calls, final QueryType queryType)
            throws TrackSearchException {

        final ExecutorService executorService = Executors.newFixedThreadPool(calls.size());
        final BaseTrackList<Track> resultTrackList = new BaseTrackList<>();

        final List<Future<BaseTrackList<Track>>> trackLists;
        try {
            trackLists = executorService.invokeAll(calls);
        } catch (InterruptedException e) {
            throw new TrackSearchException(e);
        }

        for (final Future<BaseTrackList<Track>> trackList : trackLists) {
            try {
                resultTrackList.mergeIn(trackList.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new TrackSearchException("An error occurred acquiring a tracklist", e);
            }
        }

        TrackListHelper.mergePositionValues(resultTrackList, POSITION_KEY, OFFSET_KEY);
        resultTrackList.setQueryType(queryType);
        return resultTrackList;
    }

    @Override
    public boolean hasPagingValues(@NotNull final TrackList<? extends Track> trackList) {
        return TrackListHelper.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY);
    }

}
