package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.setup.*;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.TrackFormatUtility;
import io.sfrei.tracksearch.utils.TrackListHelper;
import io.sfrei.tracksearch.utils.URLUtility;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class YouTubeClient extends SingleSearchClient<YouTubeTrack> {

    public static final String HOSTNAME = "https://youtube.com";
    private static final String INFORMATION_PREFIX = "yt";
    public static final String POSITION_KEY = INFORMATION_PREFIX + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = INFORMATION_PREFIX + TrackSearchConfig.OFFSET_KEY_SUFFIX;
    private static final String PAGING_INFORMATION = INFORMATION_PREFIX + "PagingToken";
    public static final String PAGING_KEY = "ctoken";
    private static final String ADDITIONAL_PAGING_KEY = "continuation";

    private static final Map<String, String> VIDEO_SEARCH_PARAMS = Map.of("sp", "EgIQAQ%3D%3D");
    private static final Map<String, String> TRACK_PARAMS = Map.of("pbj", "1", "hl", "en", "alt", "json");

    private static final Map<String, String> SEARCH_PARAMS;

    static {
        SEARCH_PARAMS = new HashMap<>();
        SEARCH_PARAMS.putAll(VIDEO_SEARCH_PARAMS);
        SEARCH_PARAMS.putAll(TRACK_PARAMS);
    }

    private final YouTubeService requestService;
    private final YouTubeUtility youTubeUtility;

    public YouTubeClient() {
        Retrofit base = new Retrofit.Builder()
                .baseUrl(HOSTNAME)
                .client(okHttpClient)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        requestService = base.create(YouTubeService.class);
        youTubeUtility = new YouTubeUtility();
    }

    public static Map<String, String> makeQueryInformation(String query, String pagingToken) {
        return new HashMap<>(Map.of(TrackList.QUERY_PARAM, query, PAGING_INFORMATION, pagingToken));
    }

    private BaseTrackList<YouTubeTrack> getTracksForSearch(String search, Map<String, String> params) throws TrackSearchException {
        Call<ResponseWrapper> request = requestService.getSearchForKeywords(search, params);
        ResponseWrapper response = Client.request(request);
        String content = response.getContentOrThrow();
        return youTubeUtility.getYouTubeTracks(content, QueryType.SEARCH, search, this::provideStreamUrl);
    }

    @Override
    public BaseTrackList<YouTubeTrack> getTracksForSearch(String search) throws TrackSearchException {
        BaseTrackList<YouTubeTrack> trackList = getTracksForSearch(search, SEARCH_PARAMS);
        trackList.addQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    private String provideStreamUrl(YouTubeTrack track) {
        try {
            return getStreamUrl(track);
        } catch (TrackSearchException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public BaseTrackList<YouTubeTrack> getNext(TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        if (trackList.getQueryType().equals(QueryType.SEARCH)) {
            final HashMap<String, String> params = new HashMap<>();
            params.putAll(getPagingParams(trackList.getQueryInformation()));
            params.putAll(SEARCH_PARAMS);

            BaseTrackList<YouTubeTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryParam(), params);
            return TrackListHelper.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw new YouTubeException("Query type not supported");
    }

    private YouTubeTrackInfo loadTrackInfo(YouTubeTrack youtubeTrack) throws TrackSearchException {
        Call<ResponseWrapper> trackRequest = requestService.getForUrlWithParams(youtubeTrack.getUrl(), TRACK_PARAMS);
        ResponseWrapper trackResponse = Client.request(trackRequest);

        String content = trackResponse.getContentOrThrow();
        return youtubeTrack.setAndGetTrackInfo(youTubeUtility.getTrackInfo(content));
    }

    @Override
    public String getStreamUrl(YouTubeTrack youtubeTrack) throws TrackSearchException {
        YouTubeTrackInfo trackInfo;
        if (youtubeTrack.getTrackInfo() == null)
            trackInfo = loadTrackInfo(youtubeTrack);
        else
            trackInfo = youtubeTrack.getTrackInfo();

        YouTubeTrackFormat youtubeTrackFormat = TrackFormatUtility.getBestTrackFormat(youtubeTrack, false);

        if (youtubeTrackFormat.isStreamReady())
            return youtubeTrackFormat.getUrl();

        String scriptUrl = trackInfo.getScriptUrl();
        if (scriptUrl == null)
            throw new TrackSearchException("ScriptURL could not be resolved");

        String scriptContent = Client.requestURL(HOSTNAME + scriptUrl).getContentOrThrow();

        String signatureKey = youTubeUtility.getSignature(youtubeTrackFormat, scriptContent);

        String streamUrl = youtubeTrackFormat.getUrl();
        return URLUtility.appendParam(streamUrl, youtubeTrackFormat.getSigParam(), signatureKey);
    }

    private Map<String, String> getPagingParams(Map<String, String> queryInformation) {
        String pagingToken = queryInformation.get(PAGING_INFORMATION);
        return Map.of(PAGING_KEY, pagingToken, ADDITIONAL_PAGING_KEY, pagingToken);
    }

    @Override
    public boolean hasPagingValues(TrackList<? extends Track> trackList) {
        return TrackListHelper.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY, PAGING_INFORMATION);
    }

}
