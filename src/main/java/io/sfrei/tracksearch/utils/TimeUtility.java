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
import java.time.LocalTime;

@UtilityClass
public class TimeUtility {

    private static final String TIME_STRING_DEFAULT = "00:00:00";

    public Duration getDurationForTimeString(@NonNull final String time) {

        final char[] defaultTimeStringArray = TIME_STRING_DEFAULT.toCharArray();
        final char[] reverseTimeStringArray = new StringBuffer(time).reverse().toString().toCharArray();

        for (int i = 0; i < reverseTimeStringArray.length; i++) {
            defaultTimeStringArray[defaultTimeStringArray.length - 1 - i] = reverseTimeStringArray[i];
        }

        LocalTime localTime = LocalTime.parse(String.valueOf(defaultTimeStringArray));
        LocalTime midnight = LocalTime.MIDNIGHT;
        return Duration.between(midnight, localTime);
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
