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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class OAHashSet<E> implements Set<E> {

    private static final Object DELETED = new Object() {
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
    public boolean add(E e) {

        if (e == null) {
            throw new IllegalArgumentException("add of null value is not allowed");
        }

        ++revision;
        int foundIndex = find(e);
        if (foundIndex >= 0) {
            table[foundIndex] = e;

            return false;
        }

        if (!resizeIfNeeded()) {
            foundIndex = -1 - foundIndex;
            table[foundIndex] = e;
            ++size;
            return true;
        }

        int start = e.hashCode() % table.length;

        for (int i = start; i < table.length; i++) {
            if ((table[i] == null) || (table[i] == DELETED)) {
                table[i] = e;
                ++size;
                return true;
            }
        }

        for (int i = 0; i < start; i++) {
            if ((table[i] == null) || (table[i] == DELETED)) {
                table[i] = e;
                ++size;
                return true;
            }
        }

        throw new RuntimeException("Unable to add element {" + e + "}");
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

        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {

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
        ++revision;
        Arrays.fill(table, null);
    }

    private int find(Object e) {
        if (e == null) {
            return -1;
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

        throw new RuntimeException("Unable to find insertion point for e;ement {" + e + "}");
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

        Object[] oldTable = table;
        table = new Object[newLength];

        for (Object element : oldTable) {
            if ((element != null) && (element != DELETED)) {
                add((E) element);
            }
        }

        return true;
    }
}
