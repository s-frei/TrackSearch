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

package io.sfrei.tracksearch.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringReplacer {

    public String cleanTitle(final String title) {
        return title
                .replaceAll("@", " at ")
                .replaceAll("_", " ")
                .replaceAll("\\s(\\[]\\(\\))", "")
                .replaceAll("[^\\p{javaAlphabetic}0-9&()\\[\\]\\-.\\s]", "")
                .replaceAll("\\s?HD", "")
                .replaceAll("(?i)\\(\\s?Official (Music )?Video\\s?\\)", "")
                .replaceAll("(?i)\\(\\s?Official Audio\\s?\\)", "")
                .replaceAll("(?i)\\s?\\(HQ Audio\\s?\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String replaceNonDigits(final String chars) {
        return chars.replaceAll("[^\\d.]", "");
    }

}
