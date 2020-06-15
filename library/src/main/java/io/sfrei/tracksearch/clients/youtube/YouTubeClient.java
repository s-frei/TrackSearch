package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.setup.*;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.*;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.MapUtility;
import io.sfrei.tracksearch.utils.TrackFormatUtility;
import io.sfrei.tracksearch.utils.TrackListHelper;
import io.sfrei.tracksearch.utils.URLUtility;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;

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

    private static final Map<String, String> VIDEO_SEARCH_PARAMS =  MapUtility.get("sp", "EgIQAQ%3D%3D");
    private static final Map<String, String> TRACK_PARAMS = MapUtility.get("pbj", "1", "hl", "en");

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

    @Override
    public BaseTrackList<YouTubeTrack> getTracksForSearch(String search) throws TrackSearchException {
        Map<String, String> params = MapUtility.getMerged(VIDEO_SEARCH_PARAMS, TRACK_PARAMS);
        BaseTrackList<YouTubeTrack> trackList = getTracksForSearch(search, params);
        trackList.setQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    private BaseTrackList<YouTubeTrack> getTracksForSearch(String search, Map<String, String> params) throws TrackSearchException {
        Call<ResponseWrapper> request = requestService.getSearchForKeywords(search, params);
        ResponseWrapper response = Client.request(request);
        String content = response.getContentOrThrow();
        return youTubeUtility.getYouTubeTracks(content, QueryType.SEARCH, search, this::provideStreamUrl);
    }

    @Override
    public BaseTrackList<YouTubeTrack> getNext(TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        if (trackList.getQueryType().equals(QueryType.SEARCH)) {
            Map<String, String> pagingParams = getPagingParams(trackList.getQueryInformation());

            Map<String, String> params = MapUtility.getMerged(VIDEO_SEARCH_PARAMS, TRACK_PARAMS, pagingParams);
            BaseTrackList<YouTubeTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryParam(), params);
            return TrackListHelper.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw new YouTubeException("Query type not supported");
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
    public String getStreamUrl(YouTubeTrack youtubeTrack) throws TrackSearchException {
        YouTubeTrackInfo trackInfo;
        if (youtubeTrack.getTrackInfo() == null)
            trackInfo = loadTrackInfo(youtubeTrack);
        else
            trackInfo = youtubeTrack.getTrackInfo();

        String scriptUrl = trackInfo.getScriptUrl();
        ResponseWrapper scriptResponse = Client.requestURL(HOSTNAME + scriptUrl);

        YouTubeTrackFormat youtubeTrackFormat = TrackFormatUtility.getBestTrackFormat(trackInfo);

        if (youtubeTrackFormat.isStreamReady())
            return youtubeTrackFormat.getUrl();

        String content = scriptResponse.getContentOrThrow();
        String signatureKey = youTubeUtility.getSignature(youtubeTrackFormat, content);

        String streamUrl = youtubeTrackFormat.getUrl();
        return URLUtility.appendParam(streamUrl, youtubeTrackFormat.getSigParam(), signatureKey);
    }

    private YouTubeTrackInfo loadTrackInfo(YouTubeTrack youtubeTrack) throws TrackSearchException {

        if (youtubeTrack.getTrackInfo() != null && youtubeTrack.getTrackInfo().getFormats() != null)
            return youtubeTrack.getTrackInfo();

        Call<ResponseWrapper> trackRequest = requestService.getForUrlWithParams(youtubeTrack.getUrl(), TRACK_PARAMS);
        ResponseWrapper trackResponse = Client.request(trackRequest);

        String content = trackResponse.getContentOrThrow();
        return youtubeTrack.setAndGetTrackInfo(youTubeUtility.getTrackInfo(content));
    }

    private Map<String, String> getPagingParams(Map<String, String> queryInformation) {
        String pagingToken = queryInformation.get(PAGING_INFORMATION);
        return MapUtility.get(PAGING_KEY, pagingToken, ADDITIONAL_PAGING_KEY, pagingToken);
    }

    public static Map<String, String> makeQueryInformation(String query, String pagingToken) {
        return MapUtility.get(TrackList.QUERY_PARAM, query, PAGING_INFORMATION, pagingToken);
    }

    @Override
    public boolean hasPagingValues(TrackList<? extends Track> trackList) {
        return TrackListHelper.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY, PAGING_INFORMATION);
    }

}
