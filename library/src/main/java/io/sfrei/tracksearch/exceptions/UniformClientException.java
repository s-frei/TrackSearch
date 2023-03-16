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

package io.sfrei.tracksearch.exceptions;

import io.sfrei.tracksearch.clients.interfaces.ClientHelper;
import io.sfrei.tracksearch.clients.setup.QueryType;

import java.util.function.Function;

public interface UniformClientException {

    /**
     * Exception with uniform message for failed stream URL resolving after several retries.
     *
     * @param exceptionConstructor the constructor for the exception to create.
     * @param retries              the retries which were taken.
     * @return the exception.
     */
    default TrackSearchException noStreamUrlAfterRetriesException(Function<String, TrackSearchException> exceptionConstructor,
                                                                  int retries) {
        return exceptionConstructor.apply(String.format("Not able to get stream URL after %s tries", retries + ClientHelper.INITIAL_TRY));
    }

    /**
     * Exception with uniform message when query type is not supported for some circumstances.
     *
     * @param exceptionConstructor the constructor for the exception to create.
     * @param queryType            the unsupported query type.
     * @return the exception.
     */
    default TrackSearchException unsupportedQueryTypeException(Function<String, TrackSearchException> exceptionConstructor, QueryType queryType) {
        return exceptionConstructor.apply(String.format("Query type %s not supported", queryType));
    }

}
