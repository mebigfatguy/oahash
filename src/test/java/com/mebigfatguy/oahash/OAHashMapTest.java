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
}
