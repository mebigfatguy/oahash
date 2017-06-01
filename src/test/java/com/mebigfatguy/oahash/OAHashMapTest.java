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

import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OAHashMapTest {

    private Map<String, String> m;

    @Before
    public void setUp() {
        m = new OAHashMap<>();
    }

    @Test(expected = NullPointerException.class)
    public void testPutNull() {

        m.put(null, null);
    }

    @Test
    public void testPutAndOverwrite() {
        Assert.assertSame(null, m.put("test", "tube"));
        Assert.assertEquals("tube", m.get("test"));
        Assert.assertEquals("tube", m.put("test", "exam"));
        Assert.assertEquals("exam", m.get("test"));
    }

    @Test
    public void testFillWithoutExpansion() {
        for (int i = 0; i < 11; i++) {
            String s = String.valueOf(i);
            m.put(s, s);
        }

        for (int i = 0; i < 11; i++) {
            String s = String.valueOf(i);
            Assert.assertEquals(s, m.get(s));
        }
    }

    @Test
    public void testFillWithExpansion() {
        for (int i = 0; i < 100; i++) {
            String s = String.valueOf(i);
            m.put(s, s);
        }

        for (int i = 0; i < 100; i++) {
            String s = String.valueOf(i);
            Assert.assertEquals(s, m.get(s));
        }
    }

    @Test
    public void testKeySetIteratorEqualsEntrySetIterator() {
        for (int i = 0; i < 100; i++) {
            String s = String.valueOf(i);
            m.put(s, s);
        }

        Iterator<String> ksIt = m.keySet().iterator();
        Iterator<Map.Entry<String, String>> esIt = m.entrySet().iterator();

        while (ksIt.hasNext() && esIt.hasNext()) {
            String ks = ksIt.next();
            String es = esIt.next().getKey();

            Assert.assertEquals(ks, es);
        }

        Assert.assertFalse(ksIt.hasNext());
        Assert.assertFalse(esIt.hasNext());

    }

    @Test
    public void testContains() {
        for (int i = 0; i < 100; i++) {
            String s = String.valueOf(i);
            m.put(s, s);
        }

        Assert.assertTrue(m.containsKey(String.valueOf(30)));
        Assert.assertTrue(m.containsValue(String.valueOf(40)));
        Assert.assertFalse(m.containsKey(String.valueOf(110)));
        Assert.assertFalse(m.containsValue(String.valueOf(120)));
    }

    @Test
    public void testHeavyHashCollisions() {
        Map<HashCollisionsButNotEqual, Integer> hcm = new OAHashMap<>();

        for (int i = 0; i < 100; i++) {
            HashCollisionsButNotEqual hc = new HashCollisionsButNotEqual();
            hcm.put(hc, i);
        }

        Assert.assertEquals(100, hcm.size());
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
