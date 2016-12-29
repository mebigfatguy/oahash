/*
 * baremetal4j - An open addressing hash implementation for Maps and Sets
 * Copyright 2016 MeBigFatGuy.com
 * Copyright 2016 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.oahash;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OAHashMap<K, V> implements Map<K, V> {

    private static final Object DELETED = new Object() {
    };

    private static final int DEFAULT_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.70;

    private Object[][] table;
    private int size;
    private double loadFactor;
    private int revision;

    public OAHashMap() {
        this(DEFAULT_CAPACITY);
    }

    public OAHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public OAHashMap(int initialCapacity, double initialLoadFactor) {

        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity can not be negative but was " + initialCapacity);
        }

        if ((initialLoadFactor <= 0) || (initialLoadFactor >= 100)) {
            throw new IllegalArgumentException("Initial Load Factor must be between 0 and 100 exclusively, but was " + initialLoadFactor);
        }

        table = new Object[initialCapacity][2];
        loadFactor = initialLoadFactor;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {

        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {

        int foundIndex = find(key);
        return foundIndex >= 0;
    }

    @Override
    public boolean containsValue(Object value) {

        if (size == 0) {
            return false;
        }

        for (Object[] element : table) {
            Object tableItem = element[0];
            if ((tableItem != null) && (tableItem != DELETED)) {
                tableItem = element[1];

                if (value == null) {
                    if (tableItem == null) {
                        return true;
                    }
                } else if (value.equals(tableItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public V get(Object key) {

        int foundIndex = find(key);

        if (foundIndex < 0) {
            return null;
        }

        return (V) table[foundIndex][1];
    }

    @Override
    public V put(K key, V value) {

        if (key == null) {
            throw new IllegalArgumentException("put of null key is not allowed {" + key + ", " + value + ")");
        }

        ++revision;
        int foundIndex = find(key);
        if (foundIndex >= 0) {
            V oldValue = (V) table[foundIndex][1];
            table[foundIndex][1] = value;

            return oldValue;
        }

        resizeIfNeeded();

        int start = key.hashCode() % table.length;

        for (int i = start; i < table.length; i++) {
            Object tableItem = table[i][0];
            if ((tableItem == null) || (tableItem == DELETED)) {
                table[i][0] = key;
                table[i][1] = value;
                ++size;
                return null;
            }
        }

        for (int i = 0; i < start; i++) {
            Object tableItem = table[i][0];
            if ((tableItem == null) || (tableItem == DELETED)) {
                table[i][0] = key;
                table[i][1] = value;
                ++size;
                return null;
            }
        }

        throw new RuntimeException("Unable to insert key value pair {" + key + ", " + value + "}");
    }

    @Override
    public V remove(Object key) {
        ++revision;
        int foundIndex = find(key);

        if (foundIndex < 0) {
            return null;
        }

        V value = (V) table[foundIndex][1];
        table[foundIndex][0] = DELETED;
        table[foundIndex][1] = null;
        --size;
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        ++revision;
        for (Object[] element : table) {
            Arrays.fill(element, null);
        }
    }

    @Override
    public Set<K> keySet() {

        return new OAKeySet();
    }

    @Override
    public Collection<V> values() {

        return new OAValues();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {

        return new OAEntrySet();
    }

    private int find(Object key) {
        if ((key == null) || (size == 0)) {
            return -1;
        }

        int start = key.hashCode() % table.length;
        for (int i = start; i < table.length; i++) {
            Object tableItem = table[i][0];
            if (tableItem == null) {
                return -1;
            }

            if ((tableItem != DELETED) && key.equals(tableItem)) {
                return i;
            }
        }

        for (int i = 0; i < start; i++) {
            Object tableItem = table[i][0];
            if (tableItem == null) {
                return -1;
            }

            if ((tableItem != DELETED) && key.equals(tableItem)) {
                return i;
            }
        }

        return -1;
    }

    private void resizeIfNeeded() {

        double fillPercentage = 1.0 - ((table.length - size) / ((double) table.length));

        if ((fillPercentage < loadFactor) && (table.length > size)) {
            return;
        }

        int newLength = (int) (table.length + (table.length * loadFactor));
        if (newLength <= table.length) {
            newLength += 5;
        }

        Object[][] oldTable = table;
        table = new Object[newLength][2];

        for (Object[] element : oldTable) {
            if ((element[0] != null) && (element[0] != DELETED)) {
                put((K) element[0], (V) element[1]);
            }
        }
    }

    private final class OAKeySet implements Set<K> {

        @Override
        public int size() {

            return 0;
        }

        @Override
        public boolean isEmpty() {

            return false;
        }

        @Override
        public boolean contains(Object o) {

            return false;
        }

        @Override
        public Iterator<K> iterator() {

            return null;
        }

        @Override
        public Object[] toArray() {

            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            return null;
        }

        @Override
        public boolean add(K e) {

            return false;
        }

        @Override
        public boolean remove(Object o) {

            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {

            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            return false;
        }

        @Override
        public void clear() {


        }

    }

    private final class OAValues implements Collection<V> {

        @Override
        public int size() {

            return 0;
        }

        @Override
        public boolean isEmpty() {

            return false;
        }

        @Override
        public boolean contains(Object o) {

            return false;
        }

        @Override
        public Iterator<V> iterator() {

            return null;
        }

        @Override
        public Object[] toArray() {

            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            return null;
        }

        @Override
        public boolean add(V e) {

            return false;
        }

        @Override
        public boolean remove(Object o) {

            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean addAll(Collection<? extends V> c) {

            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            return false;
        }

        @Override
        public void clear() {


        }

    }

    private final class OAEntrySet implements Set<Map.Entry<K, V>> {

        @Override
        public int size() {

            return 0;
        }

        @Override
        public boolean isEmpty() {

            return false;
        }

        @Override
        public boolean contains(Object o) {

            return false;
        }

        @Override
        public Iterator<java.util.Map.Entry<K, V>> iterator() {

            return null;
        }

        @Override
        public Object[] toArray() {

            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            return null;
        }

        @Override
        public boolean add(java.util.Map.Entry<K, V> e) {

            return false;
        }

        @Override
        public boolean remove(Object o) {

            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {

            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            return false;
        }

        @Override
        public void clear() {


        }
    }
}
