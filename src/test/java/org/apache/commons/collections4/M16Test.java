/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.collections4;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.FactoryUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.map.CompositeMap;
import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.collections4.map.FixedSizeMap;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.collections4.map.PredicatedMap;
import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.collections4.map.TransformedMap;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.junit.Test;

/**
 * LLM-style tests (M16) focused on map-related structures.
 * Designed to be fast and deterministic.
 */
public class M16Test {

    @Test
    public void defaultedMap_returnsDefaultForMissingKey_withoutInserting() {
        Map<String, Integer> base = new HashMap<>();
        Map<String, Integer> dm = DefaultedMap.defaultedMap(base, 99);
        assertEquals(Integer.valueOf(99), dm.get("missing"));
        assertFalse(dm.containsKey("missing"));
        assertTrue(base.isEmpty());
    }

    @Test
    public void defaultedMap_factory_returnsFactoryValue_forMissingKey() {
        Map<String, String> base = new HashMap<>();
        Map<String, String> dm = DefaultedMap.defaultedMap(base, FactoryUtils.constantFactory("X"));
        assertEquals("X", dm.get("nope"));
        assertFalse(dm.containsKey("nope"));
    }

    @Test
    public void lazyMap_getMissingComputesAndInserts() {
        Map<String, Integer> base = new HashMap<>();
        Map<String, Integer> lm = LazyMap.lazyMap(base, FactoryUtils.constantFactory(7));
        assertEquals(Integer.valueOf(7), lm.get("k"));
        assertTrue(lm.containsKey("k"));
        assertEquals(Integer.valueOf(7), base.get("k"));
    }

    @Test
    public void listOrderedMap_preservesInsertionOrder_inKeySetIteration() {
        Map<String, Integer> base = new HashMap<>();
        Map<String, Integer> lom = ListOrderedMap.listOrderedMap(base);
        lom.put("a", 1);
        lom.put("b", 2);
        lom.put("c", 3);
        Iterator<String> it = lom.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void multiKeyMap_storesAndRetrievesByTwoKeys() {
        MultiKeyMap<String, Integer> mk = new MultiKeyMap<>();
        mk.put("k1", "k2", 42);
        assertEquals(Integer.valueOf(42), mk.get("k1", "k2"));
        assertNull(mk.get("k1", "other"));
    }

    @Test
    public void fixedSizeMap_blocksPutForNewKey_allowsUpdateExistingKey() {
        Map<String, Integer> base = new HashMap<>();
        base.put("a", 1);
        Map<String, Integer> fsm = FixedSizeMap.fixedSizeMap(base);

        // update existing key is allowed
        assertEquals(Integer.valueOf(1), fsm.put("a", 2));
        assertEquals(Integer.valueOf(2), fsm.get("a"));

        // adding a new key is not allowed
        try {
    		fsm.put("b", 3);
    		fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
    		// ok
	}
    }

    @Test
    public void unmodifiableMap_blocksMutation() {
        Map<String, Integer> base = new HashMap<>();
        base.put("a", 1);
        Map<String, Integer> um = UnmodifiableMap.unmodifiableMap(base);
        try {
            um.put("b", 2);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        assertFalse(um.containsKey("b"));
        assertEquals(Integer.valueOf(1), um.get("a"));
    }

    @Test
    public void lruMap_evictsLeastRecentlyUsed_whenOverCapacity() {
        LRUMap<String, Integer> lru = new LRUMap<>(2);
        lru.put("a", 1);
        lru.put("b", 2);

        // make "a" most-recently-used
        assertEquals(Integer.valueOf(1), lru.get("a"));

        // insert "c" => should evict "b"
        lru.put("c", 3);

        assertTrue(lru.containsKey("a"));
        assertTrue(lru.containsKey("c"));
        assertFalse(lru.containsKey("b"));
    }

    @Test
    public void singletonMap_hasSizeOne_andReturnsKeyValue() {
        SingletonMap<String, Integer> sm = new SingletonMap<>("k", 5);
        assertEquals(1, sm.size());
        assertEquals(Integer.valueOf(5), sm.get("k"));
        assertNull(sm.get("other"));
    }

    @Test
    public void predicatedMap_rejectsNullKey() {
        Map<String, Integer> base = new HashMap<>();
        Map<String, Integer> pm = PredicatedMap.predicatedMap(
                base,
                PredicateUtils.notNullPredicate(),
                PredicateUtils.truePredicate());

        try {
            pm.put(null, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void predicatedMap_rejectsNullValue_whenNotNullPredicateUsed() {
        Map<String, Integer> base = new HashMap<>();
        Map<String, Integer> pm = PredicatedMap.predicatedMap(
                base,
                PredicateUtils.truePredicate(),
                PredicateUtils.notNullPredicate());

        try {
            pm.put("a", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void transformedMap_appliesValueTransformer_onPut() {
        Map<String, Integer> base = new HashMap<>();
        Transformer<Integer, Integer> twice = i -> i == null ? null : i * 2;
        Map<String, Integer> tm = TransformedMap.transformingMap(base, null, twice);

        tm.put("a", 3);
        assertEquals(Integer.valueOf(6), base.get("a"));
        assertEquals(Integer.valueOf(6), tm.get("a"));
    }

    @Test
    public void flat3Map_behavesLikeNormalMap_forFewEntries() {
        Flat3Map<String, Integer> m = new Flat3Map<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        assertEquals(3, m.size());
        assertEquals(Integer.valueOf(2), m.get("b"));
        assertTrue(m.containsKey("c"));
    }

    @Test
    public void compositeMap_readsFromCompositedMaps_inOrder() {
        Map<String, Integer> m1 = new HashMap<>();
        Map<String, Integer> m2 = new HashMap<>();
        m1.put("a", 1);
        m2.put("b", 2);

        CompositeMap<String, Integer> cm = new CompositeMap<>();
        cm.addComposited(m1);
        cm.addComposited(m2);

        assertEquals(Integer.valueOf(1), cm.get("a"));
        assertEquals(Integer.valueOf(2), cm.get("b"));
        assertNull(cm.get("missing"));
    }

    @Test
    public void multiValueMap_accumulatesMultipleValues_perKey() {
        MultiValueMap<String, Integer> mvm = MultiValueMap.multiValueMap(new HashMap<>());
        mvm.put("k", 1);
        mvm.put("k", 2);
        assertEquals(1, mvm.size()); // number of keys
        List<Integer> vals = Arrays.asList(mvm.getCollection("k").toArray(new Integer[0]));
        assertEquals(2, vals.size());
        assertTrue(vals.contains(1));
        assertTrue(vals.contains(2));
    }

    @Test
    public void multiValueMap_remove_removesOneValueAndKeepsOthers() {
        MultiValueMap<String, Integer> mvm = MultiValueMap.multiValueMap(new HashMap<>());
        mvm.put("k", 1);
        mvm.put("k", 2);
        assertTrue(mvm.removeMapping("k", 1));
        List<Integer> remaining = Arrays.asList(mvm.getCollection("k").toArray(new Integer[0]));
        assertEquals(1, remaining.size());
        assertEquals(Integer.valueOf(2), remaining.get(0));
    }
}
