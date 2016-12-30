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

    @Test(expected = IllegalArgumentException.class)
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
    public void testHeavyHashCollisions() {
        Map<HashCollisionsButNotEqual, HashCollisionsButNotEqual> hcm = new OAHashMap<>();

        for (int i = 0; i < 100; i++) {
            HashCollisionsButNotEqual hc = new HashCollisionsButNotEqual();
            hcm.put(hc, hc);
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
