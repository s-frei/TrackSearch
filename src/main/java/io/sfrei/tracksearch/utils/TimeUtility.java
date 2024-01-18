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

package io.sfrei.tracksearch.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class TimeUtility {

    public Duration getDurationForTimeString(@NonNull final String time) {
        String[] parts = time.split(":");

        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        switch (parts.length) {
            case 3:
                hours = Long.parseLong(parts[0]);
                minutes = Long.parseLong(parts[1]);
                seconds = Long.parseLong(parts[2]);
                break;
            case 2:
                minutes = Long.parseLong(parts[0]);
                seconds = Long.parseLong(parts[1]);
                break;
            case 1:
                seconds = Long.parseLong(parts[0]);
                break;
            default:
                throw new IllegalArgumentException(String.format("Cannot parse duration for '%s'", time));
        }

        return Duration.ofSeconds(hours * 3600 + minutes * 60 + seconds);
    }

    public Duration getDurationForMilliseconds(final Long milliseconds) {
        if (milliseconds == null)
            return null;

        return Duration.ofMillis(milliseconds);
    }

    public Duration getDurationForSeconds(final Long seconds) {
        if (seconds == null)
            return null;

        return Duration.ofSeconds(seconds);
    }

    public String formatSeconds(Duration duration) {
        final String mmss = String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());

        final long hours = duration.toHours();
        return hours > 0 ? String.format("%02d:%s", hours, mmss) : mmss;
    }

}
