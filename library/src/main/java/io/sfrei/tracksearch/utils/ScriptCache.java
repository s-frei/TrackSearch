package io.sfrei.tracksearch.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScriptCache<K, V> extends LinkedHashMap<K, V> {

    private static final byte MAX_SIZE = 20;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() >= MAX_SIZE;
    }

}
