package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.setup.ResponseWrapper;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface YouTubeService {

    @GET
    @Headers({
            TrackSearchConfig.HEADER_LANGUAGE_ENGLISH,
            TrackSearchConfig.HEADER_YOUTUBE_CLIENT_NAME,
            TrackSearchConfig.HEADER_YOUTUBE_CLIENT_VERSION
    })
    Call<ResponseWrapper> getForUrlWithParams(@Url String url,
                                              @QueryMap Map<String, String> params);

    @GET("/results")
    @Headers({
            TrackSearchConfig.HEADER_LANGUAGE_ENGLISH,
            TrackSearchConfig.HEADER_YOUTUBE_CLIENT_NAME,
            TrackSearchConfig.HEADER_YOUTUBE_CLIENT_VERSION
    })
    Call<ResponseWrapper> getSearchForKeywords(@Query("search_query") String search,
                                               @QueryMap Map<String, String> params);

    @GET("/watch")
    @Headers({
            TrackSearchConfig.HEADER_LANGUAGE_ENGLISH,
            TrackSearchConfig.HEADER_YOUTUBE_CLIENT_NAME,
            TrackSearchConfig.HEADER_YOUTUBE_CLIENT_VERSION
    })
    Call<ResponseWrapper> getVideoPage(@Query("v") String search,
                                       @QueryMap Map<String, String> params);

}
