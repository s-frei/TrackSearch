package io.sfrei.tracksearch.clients.soundcloud;


import io.sfrei.tracksearch.clients.setup.*;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.utils.TrackListHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class SoundCloudClient extends SingleSearchClient<SoundCloudTrack> {

    public static final String HOSTNAME = "https://soundcloud.com";
    private static final String INFORMATION_PREFIX = "sc";
    public static final String POSITION_KEY = INFORMATION_PREFIX + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = INFORMATION_PREFIX + TrackSearchConfig.OFFSET_KEY_SUFFIX;
    private static final String PAGING_OFFSET = "limit";
    private static final String PAGING_POSITION = "position";

    private final SoundCloudService requestService;
    private final SoundCloudUtility soundCloudUtility;

    private String clientID;

    public SoundCloudClient() {
        final Retrofit base = new Retrofit.Builder()
                .baseUrl(HOSTNAME)
                .client(okHttpClient)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        requestService = base.create(SoundCloudService.class);
        soundCloudUtility = new SoundCloudUtility();
        refreshClientID();
    }

    public static Map<String, String> makeQueryInformation(final String query) {
        return new HashMap<>(Map.of(TrackList.QUERY_PARAM, query));
    }

    private BaseTrackList<SoundCloudTrack> getTracksForSearch(final String search, int position, int offset) throws TrackSearchException {
        checkClientIDAvailableOrThrow();

        final Map<String, String> pagingParams = getPagingParams(position, offset);
        final ResponseWrapper response = getSearch(search, true, pagingParams);

        final String content = response.getContentOrThrow();
        return soundCloudUtility.getSoundCloudTracks(content, QueryType.SEARCH, search, this::provideStreamUrl);
    }

    @Override
    public TrackList<SoundCloudTrack> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        if (trackList.getQueryType().equals(QueryType.SEARCH)) {
            final int queryPosition = trackList.getQueryInformationIntValue(OFFSET_KEY);
            final int queryOffset = TrackSearchConfig.getPlaylistOffset();

            final BaseTrackList<SoundCloudTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryParam(), queryPosition, queryOffset);
            return TrackListHelper.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw new SoundCloudException("Query type not supported");
    }

    private String provideStreamUrl(final SoundCloudTrack track) {
        try {
            return getStreamUrl(track);
        } catch (TrackSearchException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public String getStreamUrl(@NonNull final SoundCloudTrack track) throws TrackSearchException {
        checkClientIDAvailableOrThrow();

        final ResponseWrapper trackResponse = getForUrlWitClientId(track.getUrl(), true);
        final Optional<String> streamUrl = soundCloudUtility.getUrlForStream(trackResponse.getContentOrThrow());
        if (streamUrl.isEmpty())
            throw new TrackSearchException("Progressive stream URL not found");

        final ResponseWrapper streamUrlResponse = getForUrlWitClientId(streamUrl.get(), true);
        return soundCloudUtility.extractStreamUrl(streamUrlResponse.getContentOrThrow());
    }

    private ResponseWrapper getSearch(final String search, final boolean firstRequest, final Map<String, String> pagingParams) {
        final Call<ResponseWrapper> request = requestService.getSearchForKeywords(search, clientID, pagingParams);

        final ResponseWrapper response = Client.request(request);
        if (!firstRequest || response.hasContent() || (response.isHttpCode(Client.UNAUTHORIZED) && !refreshClientID())) {
            return response;
        }

        return getSearch(search, false, pagingParams);
    }

    private ResponseWrapper getForUrlWitClientId(final String url, final boolean firstRequest) {
        final Call<ResponseWrapper> request = requestService.getForUrlWithClientID(url, clientID);

        final ResponseWrapper response = Client.request(request);
        if (!firstRequest || response.hasContent() || (response.isHttpCode(Client.UNAUTHORIZED) && !refreshClientID())) {
            return response;
        }
        return getForUrlWitClientId(url, false);
    }

    private void checkClientIDAvailableOrThrow() throws TrackSearchException {
        if (clientID == null && !refreshClientID())
            throw new SoundCloudException("ClientID is not available and can not be found");
    }

    public final boolean refreshClientID() {
        log.debug("SoundCloudClient -> Trying to get ClientID");
        final String clientID;
        try {
            clientID = getClientID();
        } catch (TrackSearchException e) {
            log.error(e.getMessage());
            return false;
        }
        this.clientID = clientID;
        return true;
    }

    private String getClientID() throws TrackSearchException {
        final ResponseWrapper response = Client.request(requestService.getStartPage());
        final String content = response.getContentOrThrow();
        final List<String> crossOriginScripts = soundCloudUtility.getCrossOriginScripts(content);
        for (final String scriptUrl : crossOriginScripts) {
            final ResponseWrapper scriptResponse = Client.requestURL(scriptUrl);
            if (scriptResponse.hasContent()) {
                final Optional<String> clientID = soundCloudUtility.getClientID(scriptResponse.getContent());
                if (clientID.isPresent()) {
                    return clientID.get();
                }
            }
        }
        throw new SoundCloudException("ClientId can not be found");
    }

    @Override
    public TrackList<SoundCloudTrack> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        BaseTrackList<SoundCloudTrack> trackList = getTracksForSearch(search, 0, TrackSearchConfig.getPlaylistOffset());
        trackList.addQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    private Map<String, String> getPagingParams(final int position, final int offset) {
        return Map.of(PAGING_OFFSET, String.valueOf(offset), PAGING_POSITION, String.valueOf(position));
    }

    @Override
    public boolean hasPagingValues(@NonNull final TrackList<? extends Track> trackList) {
        return TrackListHelper.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY);
    }

}
