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
import java.util.NoSuchElementException;
import java.util.Set;

public class OAHashSet<E> implements Set<E> {

    private static final Object DELETED = new Object() {
        @Override
        public String toString() {
            return "SET ENTRY DELETED";
        }
    };

    private static final int DEFAULT_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.70;
    private static final int MIN_EXPANSION = 10;

    private Object[] table;
    private int size;
    private double loadFactor;
    private int revision;

    public OAHashSet() {
        this(DEFAULT_CAPACITY);
    }

    public OAHashSet(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public OAHashSet(int initialCapacity, double initialLoadFactor) {

        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity can not be negative but was " + initialCapacity);
        }

        if ((initialLoadFactor <= 0) || (initialLoadFactor >= 100)) {
            throw new IllegalArgumentException("Initial Load Factor must be between 0 and 100 exclusively, but was " + initialLoadFactor);
        }

        table = new Object[initialCapacity];
        loadFactor = initialLoadFactor;
    }

    public OAHashSet(Set<E> source) {
        this((source == null) ? DEFAULT_CAPACITY : source.size() * (source.size() * DEFAULT_CAPACITY));

        if (source == null) {
            return;
        }

        for (E e : source) {
            addInternal(e);
        }
    }

    public OAHashSet(E... source) {
        this((source == null) ? DEFAULT_CAPACITY : source.length * (source.length * DEFAULT_CAPACITY));

        if (source == null) {
            return;
        }

        for (E e : source) {
            addInternal(e);
        }
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof Set)) {
            return false;
        }

        Set<E> that = (Set<E>) o;
        if (size != that.size()) {
            return false;
        }

        return that.containsAll(this);

    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        for (E e : this) {
            hashCode += e.hashCode();
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
    public boolean contains(Object o) {
        int foundIndex = find(o);
        return foundIndex >= 0;
    }

    @Override
    public Iterator<E> iterator() {

        return new OAHashSetIterator();
    }

    @Override
    public Object[] toArray() {

        Object[] objects = new Object[size];

        int i = 0;
        for (E e : this) {
            objects[i++] = e;
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
        for (E e : this) {
            objects[i++] = (T) e;
        }
        if (i < objects.length) {
            objects[i] = null;
        }

        return objects;
    }

    @Override
    public boolean add(E e) {

        ++revision;

        if (e == null) {
            throw new NullPointerException("add of null value is not allowed");
        }

        int foundIndex = find(e);
        if (foundIndex >= 0) {
            table[foundIndex] = e;

            return false;
        }

        if (!resizeIfNeeded(1)) {
            foundIndex = -1 - foundIndex;
            table[foundIndex] = e;
            ++size;
            return true;
        }

        addInternal(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        ++revision;
        int foundIndex = find(o);

        if (foundIndex < 0) {
            return false;
        }

        table[foundIndex] = DELETED;
        --size;
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {

        for (Object k : c) {
            if (!contains(k)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {

        ++revision;

        if (c.isEmpty()) {
            return false;
        }

        resizeIfNeeded(c.size());

        boolean modified = false;
        for (E e : c) {
            if (!contains(e)) {
                add(e);
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {

        boolean modified = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            E e = it.next();
            if (!c.contains(e)) {
                it.remove();
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {

        boolean modified = false;
        for (Object e : c) {
            modified |= remove(e);
        }

        return modified;
    }

    @Override
    public void clear() {
        ++revision;
        Arrays.fill(table, null);
        size = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        String separator = "";

        for (E e : this) {
            sb.append(separator).append(e);
            separator = ", ";
        }

        sb.append("]");

        return sb.toString();
    }

    private int find(Object e) {
        if ((e == null) || (table.length == 0)) {
            return Integer.MIN_VALUE;
        }

        int start = e.hashCode() % table.length;
        for (int i = start; i < table.length; i++) {
            if (table[i] == null) {
                return -i - 1;
            }

            if ((table[i] != DELETED) && e.equals(table[i])) {
                return i;
            }
        }

        for (int i = 0; i < start; i++) {
            if (table[i] == null) {
                return -i - 1;
            }

            if ((table[i] != DELETED) && e.equals(table[i])) {
                return i;
            }
        }

        return Integer.MIN_VALUE;
    }

    private boolean resizeIfNeeded(int expectedAdditionalItems) {

        double fillPercentage = 1.0 - ((table.length - (size + expectedAdditionalItems)) / ((double) table.length));

        if ((fillPercentage < loadFactor) && (table.length > size)) {
            return false;
        }

        int newLength = (int) (table.length + expectedAdditionalItems + (table.length * loadFactor));
        if (newLength <= (table.length + MIN_EXPANSION)) {
            newLength += MIN_EXPANSION;
        }

        size = 0;
        Object[] oldTable = table;
        table = new Object[newLength];

        for (Object element : oldTable) {
            if ((element != null) && (element != DELETED)) {
                addInternal((E) element);
            }
        }

        return true;
    }

    private void addInternal(E e) {

        if (e == null) {
            throw new NullPointerException("add of null value is not allowed");
        }

        int start = e.hashCode() % table.length;

        for (int i = start; i < table.length; i++) {
            if ((table[i] == null) || (table[i] == DELETED)) {
                table[i] = e;
                ++size;
                return;
            }
        }

        for (int i = 0; i < start; i++) {
            if ((table[i] == null) || (table[i] == DELETED)) {
                table[i] = e;
                ++size;
                return;
            }
        }

        throw new RuntimeException("Unable to add element {" + e + "}");
    }

    private final class OAHashSetIterator implements Iterator<E> {

        private int itRevision = revision;
        private int tableIndex;
        private int activeIndex;
        private boolean primed;

        public OAHashSetIterator() {
            tableIndex = -1;
            activeIndex = -1;
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
        public E next() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            findNextSlot();
            primed = false;

            if ((tableIndex < 0) || (tableIndex >= table.length)) {
                throw new NoSuchElementException();
            }

            activeIndex = tableIndex;
            return (E) table[tableIndex];
        }

        @Override
        public void remove() {
            if (itRevision != revision) {
                throw new ConcurrentModificationException();
            }

            if ((activeIndex < 0) || (activeIndex >= table.length)) {
                throw new IllegalStateException();
            }

            table[activeIndex] = DELETED;
            --size;
            tableIndex = activeIndex - 1;
            activeIndex = -1;
            primed = false;
            ++itRevision;
            ++revision;
            findNextSlot();
        }

        private void findNextSlot() {
            if (primed) {
                return;
            }

            ++tableIndex;
            while (tableIndex < table.length) {
                if ((table[tableIndex] != null) && (table[tableIndex] != DELETED)) {
                    primed = true;
                    break;
                }

                tableIndex++;
            }
        }
    }
}
