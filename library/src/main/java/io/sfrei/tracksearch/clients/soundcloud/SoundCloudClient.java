package io.sfrei.tracksearch.clients.soundcloud;


import io.sfrei.tracksearch.clients.setup.*;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.*;
import io.sfrei.tracksearch.utils.MapUtility;
import io.sfrei.tracksearch.utils.TrackListHelper;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;

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
        Retrofit base = new Retrofit.Builder()
                .baseUrl(HOSTNAME)
                .client(okHttpClient)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        requestService = base.create(SoundCloudService.class);
        soundCloudUtility = new SoundCloudUtility();
        refreshClientID();
    }

    @Override
    public TrackList<SoundCloudTrack> getTracksForSearch(String search) throws TrackSearchException {
        BaseTrackList<SoundCloudTrack> trackList = getTracksForSearch(search, 0, TrackSearchConfig.getDefaultPlaylistOffset());
        trackList.setQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    private BaseTrackList<SoundCloudTrack> getTracksForSearch(String search, int position, int offset) throws TrackSearchException {
        checkClientIDAvailableOrThrow();

        Map<String, String> pagingParams = getPagingParams(position, offset);
        ResponseWrapper response = getSearch(search, true, pagingParams);

        String content = response.getContentOrThrow();
        return soundCloudUtility.getSoundCloudTracks(content, QueryType.SEARCH, search);
    }

    @Override
    public TrackList<SoundCloudTrack> getNext(TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        if (trackList.getQueryType().equals(QueryType.SEARCH)) {
            int queryPosition = trackList.getQueryInformationValue(OFFSET_KEY);
            int queryOffset = TrackSearchConfig.getDefaultPlaylistOffset();

            BaseTrackList<SoundCloudTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryParam(), queryPosition, queryOffset);
            return TrackListHelper.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw new SoundCloudException("Query type not supported");
    }

    @Override
    public String getStreamUrl(SoundCloudTrack track) throws TrackSearchException {
        checkClientIDAvailableOrThrow();

        Optional<String> progressiveStreamUrl = getProgressiveStreamUrl(track.getUrl());
        if (!progressiveStreamUrl.isPresent())
            throw new TrackSearchException("Progressive stream URL not found");

        ResponseWrapper response = getForUrlWitClientId(progressiveStreamUrl.get(), true);
        return soundCloudUtility.getStreamUrl(response.getContentOrThrow());
    }

    private ResponseWrapper getSearch(String search, boolean firstRequest, Map<String, String> pagingParams) {
        Call<ResponseWrapper> request = requestService.getSearchForKeywords(search, clientID, pagingParams);

        ResponseWrapper response = Client.request(request);
        if (!firstRequest || response.hasContent() || (response.isHttpCode(Client.UNAUTHORIZED) && !refreshClientID())) {
            return response;
        }

        return getSearch(search, false, pagingParams);
    }

    private Optional<String> getProgressiveStreamUrl(String url) throws TrackSearchException {
        ResponseWrapper response = getForUrlWitClientId(url, true);
        if (!response.hasContent())
            return Optional.empty();
        return soundCloudUtility.getProgressiveStreamUrl(response.getContentOrThrow());
    }

    private ResponseWrapper getForUrlWitClientId(String url, boolean firstRequest) {
        Call<ResponseWrapper> request = requestService.getForUrlWithClientID(url, clientID);

        ResponseWrapper response = Client.request(request);
        if (!firstRequest || response.hasContent() || (response.isHttpCode(Client.UNAUTHORIZED) && !refreshClientID())) {
            return response;
        }
        return getForUrlWitClientId(url, false);
    }

    private void checkClientIDAvailableOrThrow() throws TrackSearchException {
        if (clientID == null && !refreshClientID())
            throw new SoundCloudException("ClientID is not available and can not be found");
    }

    public boolean refreshClientID() {
        log.debug("SoundCloudClient -> Trying to get ClientID");
        String clientID;
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
        ResponseWrapper response = Client.request(requestService.getStartPage());
        String content = response.getContentOrThrow();
        List<String> crossOriginScripts = soundCloudUtility.getCrossOriginScripts(content);
        for (String scriptUrl : crossOriginScripts) {
            ResponseWrapper scriptResponse = Client.requestURL(scriptUrl);
            if (scriptResponse.hasContent()) {
                Optional<String> clientID = soundCloudUtility.getClientID(scriptResponse.getContent());
                if (clientID.isPresent()) {
                    return clientID.get();
                }
            }
        }
        throw new SoundCloudException("ClientId can not be found");
    }

    private Map<String, String> getPagingParams(int position, int offset) {
        return MapUtility.get(PAGING_OFFSET, String.valueOf(offset), PAGING_POSITION, String.valueOf(position));
    }

    public static Map<String, String> makeQueryInformation(String query) {
        return MapUtility.get(TrackList.QUERY_PARAM, query);
    }

    @Override
    public boolean hasPagingValues(TrackList<? extends Track> trackList) {
        return TrackListHelper.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY);
    }

}
