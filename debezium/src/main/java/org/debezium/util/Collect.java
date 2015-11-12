/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of utilities for more easily creating various kinds of collections.
 */
public class Collect {

    /**
     * Create a fixed sized Map that removes the least-recently used entry when the map becomes too large. The supplied
     * {@code maximumNumberOfEntries} should be a power of 2 to efficiently make efficient use of memory. If not, the resulting
     * map will be able to contain no more than {@code maximumNumberOfEntries} entries, but the underlying map will have a
     * capacity that is the next power of larger than the supplied {@code maximumNumberOfEntries} value so that it can hold
     * the required number of entries.
     * 
     * @param maximumNumberOfEntries the maximum number of entries allowed in the map; should be a power of 2
     * @return the map that is limited in size by the specified number of entries; never null
     */
    public static <K, V> Map<K, V> fixedSizeMap(int maximumNumberOfEntries) {
        return new LinkedHashMap<K, V>(maximumNumberOfEntries + 1, .75F, true) {    // throws illegal argument if < 0
            private static final long serialVersionUID = 1L;
            final int evictionSize = maximumNumberOfEntries - 1;

            @Override
            public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > evictionSize;
            }
        };
    }

    public static <T> Set<T> unmodifiableSet(@SuppressWarnings("unchecked") T... values) {
        return unmodifiableSet(arrayListOf(values));
    }

    public static <T> Set<T> unmodifiableSet(Collection<T> values) {
        return Collections.unmodifiableSet(new HashSet<T>(values));
    }

    public static <T> Set<T> unmodifiableSet(Set<T> values) {
        return Collections.unmodifiableSet(values);
    }

    public static <T> List<T> arrayListOf(T[] values) {
        List<T> result = new ArrayList<>();
        for (T value : values) {
            if (value != null) result.add(value);
        }
        return result;
    }

    public static <T> List<T> arrayListOf(T first, @SuppressWarnings("unchecked") T... additional) {
        List<T> result = new ArrayList<>();
        result.add(first);
        for (T another : additional) {
            if (another != null) result.add(another);
        }
        return result;
    }

    public static <T> List<T> arrayListOf(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach((value) -> result.add(value));
        return result;
    }

    public static <K, V> Map<K, V> mapOf(K key, V value) {
        return Collections.singletonMap(key, value);
    }

    public static <K, V> Map<K, V> hashMapOf(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static <K, V> Map<K, V> hashMapOf(K key1, V value1, K key2, V value2) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static <K, V> Map<K, V> hashMapOf(K key1, V value1, K key2, V value2, K key3, V value3) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    public static <K, V> Map<K, V> hashMapOf(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }

    private Collect() {
    }
}
