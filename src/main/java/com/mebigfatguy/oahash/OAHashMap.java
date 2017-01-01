/*
 * oahash - An open addressing hash implementation for Maps and Sets
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class OAHashMap<K, V> implements Map<K, V> {

    private static final Object DELETED = new Object() {
    };

    private static final int DEFAULT_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.70;
    private static final int MIN_EXPANSION = 10;

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
    public boolean equals(Object o) {

        if (!(o instanceof Map)) {
            return false;
        }

        Map<K, V> that = (Map<K, V>) o;
        if (size != that.size()) {
            return false;
        }

        for (Map.Entry<K, V> entry : that.entrySet()) {
            K k = entry.getKey();
            if (k == null) {
                return false;
            }

            V v = entry.getValue();
            if (v == null) {

            } else {
                if ((get(k) == null) && containsKey(k)) {
                    return false;
                }
            }
            if (!v.equals(get(k))) {
                return false;
            }
        }

        return true;

    }

    @Override
    public int hashCode() {
        int hashCode = size;
        for (Map.Entry<K, V> entry : entrySet()) {
            hashCode ^= entry.getKey().hashCode();
        }

        return hashCode;

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

        if (!resizeIfNeeded()) {
            foundIndex = -1 - foundIndex;
            table[foundIndex][0] = key;
            table[foundIndex][1] = value;
            ++size;
            return null;
        }

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
        if (key == null) {
            return -1;
        }

        int start = key.hashCode() % table.length;
        for (int i = start; i < table.length; i++) {
            Object tableItem = table[i][0];
            if (tableItem == null) {
                return -i - 1;
            }

            if ((tableItem != DELETED) && key.equals(tableItem)) {
                return i;
            }
        }

        for (int i = 0; i < start; i++) {
            Object tableItem = table[i][0];
            if (tableItem == null) {
                return -i - 1;
            }

            if ((tableItem != DELETED) && key.equals(tableItem)) {
                return i;
            }
        }

        throw new RuntimeException("Unable to find insertion point for key {" + key + "}");
    }

    private boolean resizeIfNeeded() {

        double fillPercentage = 1.0 - ((table.length - size) / ((double) table.length));

        if ((fillPercentage < loadFactor) && (table.length > size)) {
            return false;
        }

        int newLength = (int) (table.length + (table.length * loadFactor));
        if (newLength <= (table.length + MIN_EXPANSION)) {
            newLength += MIN_EXPANSION;
        }

        size = 0;
        Object[][] oldTable = table;
        table = new Object[newLength][2];

        for (Object[] element : oldTable) {
            if ((element[0] != null) && (element[0] != DELETED)) {
                put((K) element[0], (V) element[1]);
            }
        }

        return true;
    }

    private final class OAKeySet implements Set<K> {

        @Override
        public int size() {

            return size;
        }

        @Override
        public boolean isEmpty() {

            return size == 0;
        }

        @Override
        public boolean contains(Object o) {

            return containsKey(o);
        }

        @Override
        public Iterator<K> iterator() {

            return new OAHashMapKeySetIterator();
        }

        @Override
        public Object[] toArray() {

            Object[] objects = new Object[size];

            int i = 0;
            for (K k : this) {
                objects[i++] = k;
            }

            return objects;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            T[] objects;
            if (a.length <= size) {
                objects = a;
            } else {
                objects = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            int i = 0;
            for (K k : this) {
                objects[i++] = (T) k;
            }

            return objects;
        }

        @Override
        public boolean add(K e) {

            return put(e, null) != null;
        }

        @Override
        public boolean remove(Object o) {

            return OAHashMap.this.remove(o) != null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            for (Object k : c) {
                if (!containsKey(k)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {

            boolean modified = false;
            for (K k : c) {
                if (!containsKey(k)) {
                    put(k, null);
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            boolean modified = false;
            Iterator<K> it = iterator();
            while (it.hasNext()) {
                K k = it.next();
                if (!c.contains(k)) {
                    it.remove();
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            boolean modified = false;
            for (Object k : c) {
                modified |= remove(k);
            }

            return modified;
        }

        @Override
        public void clear() {
            OAHashMap.this.clear();
        }

    }

    private final class OAValues implements Collection<V> {

        @Override
        public int size() {

            return size;
        }

        @Override
        public boolean isEmpty() {

            return size == 0;
        }

        @Override
        public boolean contains(Object o) {

            return containsValue(o);
        }

        @Override
        public Iterator<V> iterator() {

            return new OAHashMapValuesIterator();
        }

        @Override
        public Object[] toArray() {

            Object[] objects = new Object[size];

            int i = 0;
            for (V v : this) {
                objects[i++] = v;
            }

            return objects;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            T[] objects;
            if (a.length <= size) {
                objects = a;
            } else {
                objects = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            int i = 0;
            for (V v : this) {
                objects[i++] = (T) v;
            }

            return objects;
        }

        @Override
        public boolean add(V e) {

            throw new IllegalArgumentException("put of null key (via the values collection) is not allowed {value: " + e + ")");
        }

        @Override
        public boolean remove(Object o) {

            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            for (Object k : c) {
                if (!containsValue(k)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean addAll(Collection<? extends V> c) {

            throw new IllegalArgumentException("put of null keys (via the values collection) is not allowed {values: " + c + ")");
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            boolean modified = false;
            Iterator<V> it = iterator();
            while (it.hasNext()) {
                V v = it.next();
                if (c.contains(v)) {
                    it.remove();
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            boolean modified = false;
            Iterator<V> it = iterator();
            while (it.hasNext()) {
                V v = it.next();
                if (!c.contains(v)) {
                    it.remove();
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public void clear() {
            OAHashMap.this.clear();
        }

    }

    private final class OAEntrySet implements Set<Map.Entry<K, V>> {

        @Override
        public int size() {

            return size;
        }

        @Override
        public boolean isEmpty() {

            return size == 0;
        }

        @Override
        public boolean contains(Object o) {

            if (!(o instanceof Map.Entry)) {
                return false;
            }

            return containsKey(((Map.Entry) o).getKey());
        }

        @Override
        public Iterator<java.util.Map.Entry<K, V>> iterator() {

            return null;
        }

        @Override
        public Object[] toArray() {

            Object[] objects = new Object[size];

            int i = 0;
            for (Map.Entry<K, V> entry : this) {
                objects[i++] = entry;
            }

            return objects;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            T[] objects;
            if (a.length <= size) {
                objects = a;
            } else {
                objects = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            int i = 0;
            for (Map.Entry<K, V> entry : this) {
                objects[i++] = (T) entry;
            }

            return objects;
        }

        @Override
        public boolean add(java.util.Map.Entry<K, V> e) {

            return put(e.getKey(), e.getValue()) != null;
        }

        @Override
        public boolean remove(Object o) {

            if (!(o instanceof Map.Entry)) {
                return false;
            }

            return OAHashMap.this.remove(((Map.Entry) o).getKey()) != null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            throw new UnsupportedOperationException("containsAll on an entry Set collection not supported");
        }

        @Override
        public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {

            boolean modified = false;
            for (Map.Entry<K, V> entry : c) {
                if (!containsKey(entry.getKey())) {
                    put(entry.getKey(), entry.getValue());
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            throw new UnsupportedOperationException("retainAll on an entry Set collection not supported");
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            throw new UnsupportedOperationException("removeAll on an entry Set collection not supported");
        }

        @Override
        public void clear() {
            OAHashMap.this.clear();
        }
    }

    private final class OAHashMapKeySetIterator implements Iterator<K> {

        private int itRevision = revision;
        private int tableIndex;

        public OAHashMapKeySetIterator() {
            tableIndex = -1;
        }

        @Override
        public boolean hasNext() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            findNextSlot();

            return tableIndex < table.length;
        }

        @Override
        public K next() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            return (K) table[tableIndex][0];
        }

        @Override
        public void remove() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            table[tableIndex][0] = DELETED;
            table[tableIndex][1] = null;
            --size;

            ++itRevision;
            ++revision;
        }

        private void findNextSlot() {
            tableIndex++;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    break;
                }

                tableIndex++;
            }
        }
    }

    private final class OAHashMapValuesIterator implements Iterator<V> {

        private int itRevision = revision;
        private int tableIndex;

        public OAHashMapValuesIterator() {
            tableIndex = -1;
        }

        @Override
        public boolean hasNext() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            findNextSlot();

            return tableIndex < table.length;
        }

        @Override
        public V next() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            return (V) table[tableIndex][1];
        }

        @Override
        public void remove() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            table[tableIndex][0] = DELETED;
            table[tableIndex][1] = null;
            --size;
            ++itRevision;
            ++revision;
        }

        private void findNextSlot() {
            tableIndex++;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    break;
                }

                tableIndex++;
            }
        }
    }

    private final class OAHashMapEntrySetIterator implements Iterator<Map.Entry<K, V>> {

        private int itRevision = revision;
        private int tableIndex;

        public OAHashMapEntrySetIterator() {
            tableIndex = -1;
        }

        @Override
        public boolean hasNext() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            findNextSlot();

            return tableIndex < table.length;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            return new OAMapEntry();
        }

        @Override
        public void remove() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            table[tableIndex][0] = DELETED;
            table[tableIndex][1] = null;
            --size;
            ++itRevision;
            ++revision;
        }

        private void findNextSlot() {
            tableIndex++;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    break;
                }

                tableIndex++;
            }
        }

        private final class OAMapEntry implements Map.Entry<K, V> {

            @Override
            public K getKey() {
                if (itRevision != revision) {
                    throw new ConcurrentModificationException();
                }

                return (K) table[tableIndex][0];
            }

            @Override
            public V getValue() {
                if (itRevision != revision) {
                    throw new ConcurrentModificationException();
                }

                return (V) table[tableIndex][1];
            }

            @Override
            public V setValue(V value) {
                if (itRevision != revision) {
                    throw new ConcurrentModificationException();
                }

                V oldValue = (V) table[tableIndex][1];
                table[tableIndex][1] = value;
                return oldValue;
            }
        }
    }
}
