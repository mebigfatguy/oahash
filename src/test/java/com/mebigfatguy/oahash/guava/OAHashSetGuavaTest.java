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

import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.mebigfatguy.oahash.OAHashSet;

import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class OAHashSetGuavaTest {

    public static TestSuite suite() {
        return SetTestSuiteBuilder.using(new TestStringSetGenerator() {

            @Override
            protected Set<String> create(String[] entries) {
                Set<String> set = new OAHashSet<>(entries.length);
                for (String entry : entries) {
                    set.add(entry);
                }
                return set;
            }

        }).named("Guava Map Test").withFeatures(CollectionSize.ANY, CollectionFeature.RESTRICTS_ELEMENTS,
                CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION, CollectionFeature.GENERAL_PURPOSE).createTestSuite();
    }
}
