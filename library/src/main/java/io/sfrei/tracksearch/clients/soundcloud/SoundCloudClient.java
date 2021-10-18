package io.sfrei.tracksearch.clients.soundcloud;


import io.sfrei.tracksearch.clients.helper.ClientHelper;
import io.sfrei.tracksearch.clients.setup.*;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.config.TrackSearchConstants;
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

import java.net.CookiePolicy;
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

        super(CookiePolicy.ACCEPT_ALL, null);

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
            final int queryOffset = TrackSearchConfig.DEFAULT_PLAYLIST_OFFSET;

            final BaseTrackList<SoundCloudTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryParam(), queryPosition, queryOffset);
            return TrackListHelper.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw new SoundCloudException("Query type not supported");
    }

    private String provideStreamUrl(final SoundCloudTrack track) {
        try {
            return getStreamUrl(track, TrackSearchConstants.RETRY_RESOLVING_ONCE);
        } catch (TrackSearchException e) {
            log.error("Error occurred acquiring stream URL", e);
        }
        return null;
    }

    @Override
    public String getStreamUrl(@NonNull final SoundCloudTrack soundCloudTrack) throws TrackSearchException {
        checkClientIDAvailableOrThrow();

        final ResponseWrapper trackResponse = getForUrlWitClientId(soundCloudTrack.getUrl(), true);
        final Optional<String> streamUrl = soundCloudUtility.getUrlForStream(trackResponse.getContentOrThrow());
        if (streamUrl.isEmpty())
            throw new TrackSearchException("Progressive stream URL not found");

        final ResponseWrapper streamUrlResponse = getForUrlWitClientId(streamUrl.get(), true);
        return soundCloudUtility.extractStreamUrl(streamUrlResponse.getContentOrThrow());
    }

    @Override
    public String getStreamUrl(@NonNull SoundCloudTrack soundCloudTrack, final int retries) throws TrackSearchException {
        return ClientHelper.getStreamUrl(this, soundCloudTrack, this::requestAndGetCode, retries)
                .orElseThrow(() -> new SoundCloudException(String.format("Not able to get stream URL after %s retries", retries)));
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
        log.debug("Trying to get ClientID");
        final String clientID;
        try {
            clientID = getClientID();
        } catch (TrackSearchException e) {
            log.error("Cannot refresh client ID", e);
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
            final ResponseWrapper scriptResponse = requestURL(scriptUrl);
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
        BaseTrackList<SoundCloudTrack> trackList = getTracksForSearch(search, 0, TrackSearchConfig.DEFAULT_PLAYLIST_OFFSET);
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
