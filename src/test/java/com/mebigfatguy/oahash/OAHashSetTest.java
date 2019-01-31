/*
 * oahash - An open addressing hash implementation for Maps and Sets
 * Copyright 2016-2019 MeBigFatGuy.com
 * Copyright 2016-2019 Dave Brosius
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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class OAHashSetTest {

    @Test(expected = NullPointerException.class)
    public void testPutNull() {
        Set<String> s = new OAHashSet<>();
        s.add(null);
    }

    @Test
    public void testAddAndOverwrite() {
        Set<String> s = new OAHashSet<>();
        Assert.assertTrue(s.add("test"));
        Assert.assertFalse(s.add("test"));
    }

    @Test
    public void testFillWithoutExpansion() {
        Set<String> s = new OAHashSet<>();
        for (int i = 0; i < 11; i++) {
            String o = String.valueOf(i);
            s.add(o);
        }

        for (int i = 0; i < 11; i++) {
            String o = String.valueOf(i);
            Assert.assertTrue(s.contains(o));
        }
    }

    @Test
    public void testFillWithExpansion() {
        Set<String> s = new OAHashSet<>();
        for (int i = 0; i < 100; i++) {
            String o = String.valueOf(i);
            s.add(o);
        }

        for (int i = 0; i < 100; i++) {
            String o = String.valueOf(i);
            Assert.assertTrue(s.contains(o));
        }
    }

    @Test
    public void testAddAll() {

        Set<String> dest = new OAHashSet<>();

        for (int i = 0; i < 10; i++) {
            Set<String> src = new OAHashSet<>();
            for (int j = 0; j < 100; j++) {
                src.add(i + "-" + j);
            }

            dest.addAll(src);
        }

        Assert.assertEquals(1000, dest.size());
    }

    @Test
    public void testHeavyHashCollisions() {
        Set<String> s = new OAHashSet<>();
        Set<HashCollisionsButNotEqual> hcs = new OAHashSet<>();

        for (int i = 0; i < 100; i++) {
            HashCollisionsButNotEqual hc = new HashCollisionsButNotEqual();
            hcs.add(hc);
        }

        Assert.assertEquals(100, hcs.size());
    }

    @Test
    public void testCountingAndDeletingIterator() {
        Set<String> s = new OAHashSet<>();
        for (int i = 0; i < 20; i++) {
            s.add(String.valueOf(i));
        }

        int count = 0;
        Iterator<String> it = s.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }

        Assert.assertEquals(20, count);

        it = s.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }

        Assert.assertEquals(0, s.size());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testCMEIterator() {
        Set<String> s = new OAHashSet<>();
        for (int i = 0; i < 20; i++) {
            s.add(String.valueOf(i));
        }

        Iterator<String> it = s.iterator();
        it = s.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
            s.remove(String.valueOf(10));
        }
    }

    static class HashCollisionsButNotEqual {
        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }
}
