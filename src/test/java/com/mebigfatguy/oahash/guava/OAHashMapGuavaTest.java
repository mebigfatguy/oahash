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
package com.mebigfatguy.oahash.guava;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.mebigfatguy.oahash.OAHashMap;

import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class OAHashMapGuavaTest {

    public static TestSuite suite() {
        return MapTestSuiteBuilder.using(new TestStringMapGenerator() {

            @Override
            protected Map<String, String> create(Entry<String, String>[] entries) {
                Map<String, String> map = new OAHashMap<>(entries.length);
                for (Entry<String, String> entry : entries) {
                    map.put(entry.getKey(), entry.getValue());
                }
                return map;
            }

        }).named("Guava Map Test").withFeatures(CollectionSize.ANY, CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION, MapFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUPPORTS_ADD, MapFeature.GENERAL_PURPOSE).createTestSuite();
    }
}
