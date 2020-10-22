package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.QueryType;

import java.util.List;
import java.util.Map;

public interface TrackList<T extends Track> {

    String QUERY_PARAM = "query";

    QueryType getQueryType();

    Map<String, String> getQueryInformation();

    Integer getQueryInformationIntValue(String key);

    void mergeIn(BaseTrackList<T> from);

    List<T> getTracks();

    boolean isEmpty();

    String getQueryParam();

}
