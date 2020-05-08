package io.sfrei.tracksearch.utils;

import java.util.HashMap;
import java.util.Map;

public class MapUtility {

    @SafeVarargs
    public static <K, V> Map<K, V> getMerged(Map<K, V>... maps) {
        Map<K, V> result = new HashMap<>();
        for (Map<K, V> map : maps) {
            result.putAll(map);
        }
        return result;
    }

    public static <K, V> void set(Map<K, V> map, K key1, V value1, K key2, V value2) {
        map.put(key1, value1);
        map.put(key2, value2);
    }

    public static <K, V> Map<K, V> get(K key1, V value1, K key2, V value2) {
        Map<K, V> result = new HashMap<>();
        set(result, key1, value1, key2, value2);
        return result;
    }

    public static <K, V> Map<K, V> get(K key1, V value1) {
        Map<K, V> result = new HashMap<>();
        result.put(key1, value1);
        return result;
    }

}
