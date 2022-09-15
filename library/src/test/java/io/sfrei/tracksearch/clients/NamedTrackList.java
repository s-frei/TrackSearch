package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.TrackList;

import java.util.List;
import java.util.Map;

public class NamedTrackList implements TrackList {
    @Override
    public QueryType getQueryType() {
        return null;
    }

    @Override
    public Map<String, String> getQueryInformation() {
        return null;
    }

    @Override
    public Integer getQueryInformationIntValue(String key) {
        return null;
    }

    @Override
    public void mergeIn(BaseTrackList from) {

    }

    @Override
    public List getTracks() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String getQueryParam() {
        return null;
    }
}
