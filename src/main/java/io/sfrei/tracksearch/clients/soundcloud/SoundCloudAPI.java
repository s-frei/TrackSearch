/*
 * Copyright (C) 2024 s-frei (sfrei.io)
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

package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.common.ResponseWrapper;
import io.sfrei.tracksearch.clients.common.SharedClient;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface SoundCloudAPI {

    String HEADER_SOUNDCLOUD_REFERER = SoundCloudClient.URL + "/";
    String HEADER_SOUNDCLOUD_ORIGIN = SoundCloudClient.URL;

    @GET("/")
    @Headers({
            SharedClient.HEADER_LANGUAGE_ENGLISH,
            HEADER_SOUNDCLOUD_REFERER,
            HEADER_SOUNDCLOUD_ORIGIN
    })
    Call<ResponseWrapper> getStartPage();

    @GET
    @Headers({
            SharedClient.HEADER_LANGUAGE_ENGLISH,
            HEADER_SOUNDCLOUD_REFERER,
            HEADER_SOUNDCLOUD_ORIGIN
    })
    Call<ResponseWrapper> getForUrlWithClientID(
            @Url String url,
            @Query("client_id") String clientID
    );

    @GET("https://api-v2.soundcloud.com/search/tracks")
    @Headers({
            SharedClient.HEADER_LANGUAGE_ENGLISH,
            HEADER_SOUNDCLOUD_REFERER,
            HEADER_SOUNDCLOUD_ORIGIN
    })
    Call<ResponseWrapper> getSearchForKeywords(
            @Query("q") String search,
            @Query("client_id") String clientID,
            @QueryMap Map<String, String> pagingParams
    );

}
