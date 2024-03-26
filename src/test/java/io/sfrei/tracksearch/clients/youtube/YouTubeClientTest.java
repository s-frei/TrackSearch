/*
 * Copyright (C) 2023 s-frei (sfrei.io)
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

import io.sfrei.tracksearch.clients.ClientTest;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import org.junit.jupiter.api.Tag;

import java.util.List;

@Tag("ClientTest")
public class YouTubeClientTest extends ClientTest<YouTubeClient, YouTubeTrack> {

    public static final List<String> TRACK_URLS = List.of(
            "https://www.youtube.com/watch?v=yZrQBa3BBBE",
            "https://www.youtube.com/watch?v=MXlAU-HT4Os",
            "https://www.youtube.com/watch?v=Kohoxm8NwRA"

    );

    public YouTubeClientTest() {
        super(new YouTubeClient(), true);
    }

    @Override
    public List<String> trackURLs() {
        return TRACK_URLS;
    }

}
