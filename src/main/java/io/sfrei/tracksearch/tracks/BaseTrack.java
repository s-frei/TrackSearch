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

package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.TrackSource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Duration;

@Getter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseTrack implements Track {

    private final TrackSource source;

    private final String title;

    private final Duration duration;

    @EqualsAndHashCode.Include
    private final String url;

    private String pretty(String title) {
        return String.format("[%s] (%s) '%s' - %s", source.name(), durationFormatted(), title, url);
    }

    public String pretty() {
        return pretty(title);
    }

    public String prettyAndClean() {
        return pretty(getCleanTitle());
    }

}
