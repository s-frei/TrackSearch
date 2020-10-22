package io.sfrei.tracksearch.clients;

import java.util.ArrayList;
import java.util.List;

public class ClientTestConstants {

    //Amazing german artist ;)
    public static final String SEARCH_KEY = "Ben Böhmer | Live at Anjunadeep x Printworks London 2019 (Official HD Set)";

    public static final List<String> SEARCH_KEYS;

    static {
        SEARCH_KEYS = new ArrayList<>();
        SEARCH_KEYS.add(SEARCH_KEY);
        SEARCH_KEYS.add("Paul Kalkbrenner");
        SEARCH_KEYS.add("Einmusik");
        SEARCH_KEYS.add("Mind Against");
        SEARCH_KEYS.add("Adriatique");
        SEARCH_KEYS.add("Fideles");
    }

}
