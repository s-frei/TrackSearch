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

package io.sfrei.tracksearch.tracks.metadata;

public interface TrackMetadata {

    /**
     * Get the name of the publishers channel.
     *
     * @return the channel name.
     */
    String channelName();

    /**
     * Get the URL of the publishers channel.
     *
     * @return the channel URL.
     */
    String channelUrl();

    /**
     * Get the amount of streams.
     *
     * @return the stream amount.
     */
    Long streamAmount();

    /**
     * Get the URL of the media thumbnail (small).
     *
     * @return the thumbnail URL.
     */
    String thumbNailUrl();

}
