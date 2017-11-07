/*
 * oahash - An open addressing hash implementation for Maps and Sets
 * Copyright 2016-2017 MeBigFatGuy.com
 * Copyright 2016-2017 Dave Brosius
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
import java.util.Objects;
import java.util.Set;

public class OAHashMap<K, V> implements Map<K, V> {

    private static final Object DELETED = new Object() {
        @Override
        public String toString() {
            return "MAP ENTRY DELETED";
        }
    };

    private static final int DEFAULT_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.70;
    private static final int MIN_EXPANSION = 20; // 10 slots

    private Object[] table; // odd indices are the key, even indices are the values
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

        table = new Object[initialCapacity * 2];
        loadFactor = initialLoadFactor;
    }

    public OAHashMap(Map<K, V> source) {
        this((source == null) ? DEFAULT_CAPACITY : source.size() * (source.size() * DEFAULT_CAPACITY));

        if (source == null) {
            return;
        }

        for (Map.Entry<K, V> entry : source.entrySet()) {
            putInternal(entry.getKey(), entry.getValue());
        }
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
                if ((get(k) != null) || !containsKey(k)) {
                    return false;
                }
            } else if (!v.equals(get(k))) {
                return false;
            }
        }

        return true;

    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        for (int i = 0; i < table.length; i += 2) {
            K key = (K) table[i];
            if ((key != null) && (key != DELETED)) {
                V value = (V) table[i + 1];

                hashCode += Objects.hashCode(key) ^ Objects.hashCode(value);
            }
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

        for (int i = 0; i < table.length; i += 2) {
            Object tableItem = table[i];
            if ((tableItem != null) && (tableItem != DELETED)) {
                tableItem = table[i + 1];

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

        return (V) table[foundIndex + 1];
    }

    @Override
    public V put(K key, V value) {

        ++revision;

        if (key == null) {
            throw new NullPointerException("put of null key is not allowed {null, " + value + ")");
        }

        int foundIndex = find(key);
        if (foundIndex >= 0) {
            int valueIndex = foundIndex + 1;
            V oldValue = (V) table[valueIndex];
            table[valueIndex] = value;

            return oldValue;
        }

        if (!resizeIfNeeded(1)) {
            foundIndex = -1 - foundIndex;
            table[foundIndex++] = key;
            table[foundIndex] = value;
            ++size;
            return null;
        }

        putInternal(key, value);
        return null;
    }

    @Override
    public V remove(Object key) {
        ++revision;
        int foundIndex = find(key);

        if (foundIndex < 0) {
            return null;
        }

        V value = (V) table[foundIndex + 1];
        table[foundIndex++] = DELETED;
        table[foundIndex] = null;
        --size;
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        ++revision;

        if (m.isEmpty()) {
            return;
        }

        resizeIfNeeded(m.size());

        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        ++revision;
        Arrays.fill(table, null);
        size = 0;
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

    public Object[][] toArray() {

        Object[][] objects = new Object[size][2];

        int i = 0;
        for (Map.Entry<K, V> entry : entrySet()) {
            objects[i][0] = entry.getKey();
            objects[i++][1] = entry.getValue();
        }

        return objects;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        String separator = "";

        for (Map.Entry<K, V> entry : entrySet()) {
            sb.append(separator).append(entry.getKey()).append('=').append(entry.getValue());
            separator = ", ";
        }

        sb.append('}');

        return sb.toString();
    }

    private int find(Object key) {
        if ((key == null) || (table.length == 0)) {
            return Integer.MIN_VALUE;
        }

        int start = (key.hashCode() % (table.length >> 1)) << 1;
        for (int i = start; i < table.length; i += 2) {
            Object tableItem = table[i];
            if (tableItem == null) {
                return -i - 1;
            }

            if ((tableItem != DELETED) && key.equals(tableItem)) {
                return i;
            }
        }

        for (int i = 0; i < start; i += 2) {
            Object tableItem = table[i];
            if (tableItem == null) {
                return -i - 1;
            }

            if ((tableItem != DELETED) && key.equals(tableItem)) {
                return i;
            }
        }

        return Integer.MIN_VALUE;
    }

    private boolean resizeIfNeeded(int expectedAdditionalItems) {

        int slotCount = table.length >> 1;
        double fillPercentage = 1.0 - ((slotCount - (size + expectedAdditionalItems)) / ((double) slotCount));

        if ((fillPercentage < loadFactor) && (table.length > size)) {
            return false;
        }

        int newLength;
        int proposedLength = ((int) (slotCount + expectedAdditionalItems + (slotCount * loadFactor))) << 1;
        int minNewSize = table.length + MIN_EXPANSION;
        if (proposedLength <= minNewSize) {
            newLength = minNewSize;
        } else {
            newLength = proposedLength;
        }

        size = 0;
        Object[] oldTable = table;
        table = new Object[newLength];

        for (int i = 0; i < oldTable.length; i += 2) {
            if ((oldTable[i] != null) && (oldTable[i] != DELETED)) {
                putInternal((K) oldTable[i], (V) oldTable[i + 1]);
            }
        }

        return true;
    }

    private void putInternal(K key, V value) {

        if (key == null) {
            throw new NullPointerException("put of null key is not allowed {null, " + value + ")");
        }

        int start = (key.hashCode() % (table.length >> 1)) << 1;

        for (int i = start; i < table.length; i += 2) {
            if ((table[i] == null) || (table[i] == DELETED)) {
                table[i++] = key;
                table[i] = value;
                ++size;
                return;
            }
        }

        for (int i = 0; i < start; i += 2) {
            if ((table[i] == null) || (table[i] == DELETED)) {
                table[i++] = key;
                table[i] = value;
                ++size;
                return;
            }
        }

        throw new RuntimeException("Unable to insert key value pair {" + key + ", " + value + "}");
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
            if (a.length >= size) {
                objects = a;
            } else {
                objects = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            int i = 0;
            for (K k : this) {
                objects[i++] = (T) k;
            }
            if (i < objects.length) {
                objects[i] = null;
            }

            return objects;
        }

        @Override
        public boolean add(K e) {
            throw new UnsupportedOperationException("add");
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
            throw new UnsupportedOperationException("addAll");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            ++revision;

            if (c.isEmpty()) {
                boolean wasEmpty = isEmpty();
                OAHashMap.this.clear();
                return !wasEmpty;
            }

            boolean modified = false;
            for (int i = 0; i < table.length; i += 2) {
                final K key = (K) table[i];

                if ((key != null) && (key != DELETED)) {

                    if (!c.contains(key)) {
                        table[i] = DELETED;
                        table[i + 1] = null;
                        --size;
                        modified = true;
                    }
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

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (Map.Entry<K, V> entry : entrySet()) {
                hashCode += Objects.hashCode(entry.getKey());
            }

            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Set)) {
                return false;
            }

            Set<K> that = (Set<K>) o;
            if (size != that.size()) {
                return false;
            }

            for (K k : that) {
                if (!containsKey(k)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            String separator = "";

            for (Map.Entry<K, V> entry : entrySet()) {
                sb.append(separator).append(entry.getKey());
                separator = ", ";
            }

            sb.append(']');

            return sb.toString();
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
            if (a.length >= size) {
                objects = a;
            } else {
                objects = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            int i = 0;
            for (V v : this) {
                objects[i++] = (T) v;
            }
            if (i < objects.length) {
                objects[i] = null;
            }

            return objects;
        }

        @Override
        public boolean add(V e) {
            throw new UnsupportedOperationException("add");
        }

        @Override
        public boolean remove(Object o) {
            ++revision;

            boolean modified = false;
            for (int i = 0; i < table.length; i += 2) {
                final K key = (K) table[i];

                if ((key != null) && (key != DELETED)) {
                    final V value = (V) table[i + 1];

                    if (Objects.equals(value, o)) {
                        table[i] = DELETED;
                        table[i + 1] = null;
                        --size;
                        modified = true;
                    }
                }
            }

            return modified;
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
            throw new UnsupportedOperationException("addAll");
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            boolean modified = false;
            OAHashMapValuesIterator it = (OAHashMapValuesIterator) iterator();
            while (it.hasNext()) {
                V v = it.next();
                if (c.contains(v)) {
                    it.removeInternal();
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            ++revision;

            if (c.isEmpty()) {
                boolean wasEmpty = isEmpty();
                OAHashMap.this.clear();
                return !wasEmpty;
            }

            boolean modified = false;
            for (int i = 0; i < table.length; i += 2) {
                final K key = (K) table[i];

                if ((key != null) && (key != DELETED)) {
                    final V value = (V) table[i + 1];

                    if (!c.contains(value)) {
                        table[i] = DELETED;
                        table[i + 1] = null;
                        --size;
                        modified = true;
                    }
                }
            }

            return modified;
        }

        @Override
        public void clear() {
            OAHashMap.this.clear();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            String separator = "";

            for (Map.Entry<K, V> entry : entrySet()) {
                sb.append(separator).append(entry.getValue());
                separator = ", ";
            }

            sb.append(']');

            return sb.toString();
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

            if (!containsKey(((Map.Entry<K, V>) o).getKey())) {
                return false;
            }
            V value = get(((Map.Entry<K, V>) o).getKey());

            return Objects.equals(value, ((Map.Entry<K, V>) o).getValue());
        }

        @Override
        public Iterator<java.util.Map.Entry<K, V>> iterator() {

            return new OAHashMapEntrySetIterator();
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
            if (a.length >= size) {
                objects = a;
            } else {
                objects = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }

            int i = 0;
            for (Map.Entry<K, V> entry : this) {
                objects[i++] = (T) entry;
            }
            if (i < objects.length) {
                objects[i] = null;
            }

            return objects;
        }

        @Override
        public boolean add(java.util.Map.Entry<K, V> e) {
            throw new UnsupportedOperationException("add");
        }

        @Override
        public boolean remove(Object o) {

            if (!(o instanceof Map.Entry)) {
                return false;
            }

            return OAHashMap.this.remove(((Map.Entry<K, V>) o).getKey()) != null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            for (Object e : c) {
                K k = (K) ((Map.Entry) e).getKey();
                V v = (V) ((Map.Entry) e).getValue();

                if (!containsKey(k)) {
                    return false;
                }
                V existingV = get(k);
                if (!Objects.equals(v, existingV)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
            throw new UnsupportedOperationException("addAll");
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            ++revision;

            if (c.isEmpty()) {
                boolean wasEmpty = isEmpty();
                OAHashMap.this.clear();
                return !wasEmpty;
            }

            boolean modified = false;
            for (int i = 0; i < table.length; i += 2) {
                final K key = (K) table[i];

                if ((key != null) && (key != DELETED)) {

                    if (!c.contains(new OAMapEntry(revision, i))) {
                        table[i] = DELETED;
                        table[i + 1] = null;
                        --size;
                        modified = true;
                    }
                }
            }

            return modified;
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            boolean modified = false;
            for (Object o : c) {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
                modified |= (OAHashMap.this.remove(entry.getKey()) != null);
            }
            return modified;
        }

        @Override
        public void clear() {
            OAHashMap.this.clear();
        }

        @Override
        public int hashCode() {
            return OAHashMap.this.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Set)) {
                return false;
            }
            Collection<? extends java.util.Map.Entry<K, V>> c = (Collection<? extends java.util.Map.Entry<K, V>>) o;

            if (size != c.size()) {
                return false;
            }

            for (Map.Entry<K, V> e : c) {
                if (!contains(e)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            String separator = "";

            for (Map.Entry<K, V> entry : entrySet()) {
                sb.append(separator).append(entry.getKey()).append('=').append(entry.getValue());
                separator = ", ";
            }

            sb.append(']');

            return sb.toString();
        }
    }

    private final class OAHashMapKeySetIterator implements Iterator<K> {

        private int itRevision = revision;
        private int tableIndex;
        private int activeIndex;
        private boolean primed;

        public OAHashMapKeySetIterator() {
            tableIndex = -2;
            activeIndex = -2;
            primed = false;
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

            findNextSlot();
            primed = false;

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            activeIndex = tableIndex;
            return (K) table[tableIndex];
        }

        @Override
        public void remove() {

            throw new UnsupportedOperationException("remove");
        }

        private void removeInternal() {

            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((activeIndex < 0) || (activeIndex >= table.length)) {
                throw new IllegalStateException();
            }

            table[activeIndex] = DELETED;
            table[activeIndex + 1] = null;
            --size;
            tableIndex = activeIndex - 2;
            activeIndex = -2;
            primed = false;
            ++itRevision;
            ++revision;
            findNextSlot();
        }

        private void findNextSlot() {
            if (primed) {
                return;
            }

            tableIndex += 2;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    primed = true;
                    break;
                }

                tableIndex += 2;
            }
        }
    }

    private final class OAHashMapValuesIterator implements Iterator<V> {

        private int itRevision = revision;
        private int tableIndex;
        private int activeIndex;
        private boolean primed;

        public OAHashMapValuesIterator() {
            tableIndex = -2;
            activeIndex = -2;
            primed = false;
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

            findNextSlot();
            primed = false;

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            activeIndex = tableIndex;
            return (V) table[tableIndex + 1];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
            /*
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }
            
            if ((activeIndex < 0) || (activeIndex >= table.length)) {
                throw new IllegalStateException();
            }
            
            table[tableIndex] = DELETED;
            table[tableIndex + 1] = null;
            --size;
            tableIndex = activeIndex - 2;
            activeIndex = -2;
            primed = false;
            ++itRevision;
            ++revision;
            findNextSlot();
            */
        }

        private void removeInternal() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((activeIndex < 0) || (activeIndex >= table.length)) {
                throw new IllegalStateException();
            }

            table[tableIndex] = DELETED;
            table[tableIndex + 1] = null;
            --size;
            tableIndex = activeIndex - 2;
            activeIndex = -2;
            primed = false;
            ++itRevision;
            ++revision;
            findNextSlot();
        }

        private void findNextSlot() {
            if (primed) {
                return;
            }

            tableIndex += 2;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    primed = true;
                    break;
                }

                tableIndex += 2;
            }
        }
    }

    private final class OAHashMapEntrySetIterator implements Iterator<Map.Entry<K, V>> {

        private int itRevision = revision;
        private int tableIndex;
        private boolean primed;

        public OAHashMapEntrySetIterator() {
            tableIndex = -2;
            primed = false;
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

            findNextSlot();
            primed = false;

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            return new OAMapEntry(itRevision, tableIndex);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void findNextSlot() {
            if (primed) {
                return;
            }

            tableIndex += 2;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    primed = true;
                    break;
                }

                tableIndex += 2;
            }
        }
    }

    private final class OAMapEntry implements Map.Entry<K, V> {

        private int entryRevision;
        private int entryIndex;

        public OAMapEntry(int revision, int index) {
            entryRevision = revision;
            entryIndex = index;
        }

        @Override
        public K getKey() {
            if (entryRevision != revision) {
                throw new ConcurrentModificationException();
            }

            return (K) table[entryIndex];
        }

        @Override
        public V getValue() {
            if (entryRevision != revision) {
                throw new ConcurrentModificationException();
            }

            return (V) table[entryIndex + 1];
        }

        @Override
        public V setValue(V value) {
            if (entryRevision != revision) {
                throw new ConcurrentModificationException();
            }

            V oldValue = (V) table[entryIndex + 1];
            table[entryIndex + 1] = value;
            return oldValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(table[entryIndex]) ^ Objects.hashCode(table[entryIndex + 1]);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry that = (Map.Entry) o;

            return Objects.equals(table[entryIndex], that.getKey()) && Objects.equals(table[entryIndex + 1], that.getValue());
        }

        @Override
        public String toString() {
            return "[" + table[entryIndex] + "=" + table[entryIndex + 1] + "]";
        }
    }
}
