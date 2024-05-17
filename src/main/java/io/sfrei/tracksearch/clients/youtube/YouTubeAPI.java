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

package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.common.ResponseWrapper;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface YouTubeAPI {

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

}
