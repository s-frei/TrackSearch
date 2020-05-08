package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.setup.ResponseWrapper;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

import java.util.Map;

public interface SoundCloudService {

    @GET("/")
    Call<ResponseWrapper> getStartPage();

    @GET
    Call<ResponseWrapper> getForUrlWithClientID(@Url String url,
                                                @Query("client_id") String clientID);

    @GET("https://api-v2.soundcloud.com/search/tracks")
    Call<ResponseWrapper> getSearchForKeywords(@Query("q") String search,
                                               @Query("client_id") String clientID,
                                               @QueryMap Map<String, String> pagingParams);

}
