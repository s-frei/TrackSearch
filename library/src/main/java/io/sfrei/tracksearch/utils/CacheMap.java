package io.sfrei.tracksearch.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CacheMap<K, V> extends LinkedHashMap<K, V> {

    public static final byte MAX_SIZE = 50;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() >= MAX_SIZE;
    }

}
