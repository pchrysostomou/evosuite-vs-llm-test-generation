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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections4.FactoryUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.list.FixedSizeList;
import org.apache.commons.collections4.list.GrowthList;
import org.apache.commons.collections4.list.LazyList;
import org.apache.commons.collections4.list.PredicatedList;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.collections4.list.TransformedList;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.junit.Test;

/**
 * LLM-style tests (M32) focused on list-related structures.
 * Designed to be fast and deterministic.
 */
public class M32Test {

    private static <T> List<T> arrayList(final T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static void expectUOE(final Runnable r) {
        try {
            r.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    private static void expectIAE(final Runnable r) {
        try {
            r.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void lazyList_getBeyondSize_expandsAndReturnsFactoryValue() {
        List<String> base = arrayList("a");
        List<String> ll = LazyList.lazyList(base, FactoryUtils.constantFactory("x"));
        assertEquals("x", ll.get(3));
        assertEquals(4, ll.size());
        assertEquals("a", ll.get(0));
    }

    @Test
    public void lazyList_getWithinSize_returnsExistingElement() {
        List<Integer> base = arrayList(1, 2, 3);
        List<Integer> ll = LazyList.lazyList(base, FactoryUtils.constantFactory(9));
        assertEquals(Integer.valueOf(2), ll.get(1));
        assertEquals(3, ll.size());
    }

    @Test
    public void lazyList_setAfterExpansion_overwritesElement() {
        List<String> base = arrayList();
        List<String> ll = LazyList.lazyList(base, FactoryUtils.constantFactory("z"));
        assertEquals("z", ll.get(2));
        ll.set(2, "k");
        assertEquals("k", ll.get(2));
    }

    @Test
    public void lazyList_transformerBased_generatesIndexBasedValues() {
        Transformer<Integer, String> t = i -> "v" + i;
        List<String> base = arrayList();
        List<String> ll = LazyList.lazyList(base, t);
        assertEquals("v5", ll.get(5));
        assertEquals(6, ll.size());
    }

    @Test
    public void fixedSizeList_addIsBlocked() {
        List<String> fs = FixedSizeList.fixedSizeList(arrayList("a", "b"));
        expectUOE(() -> fs.add("c"));
        assertEquals(2, fs.size());
    }

    @Test
    public void fixedSizeList_removeIsBlocked() {
        List<Integer> fs = FixedSizeList.fixedSizeList(arrayList(1, 2, 3));
        expectUOE(() -> fs.remove(0));
        assertEquals(3, fs.size());
    }

    @Test
    public void fixedSizeList_setIsAllowed() {
        List<String> fs = FixedSizeList.fixedSizeList(arrayList("a", "b"));
        fs.set(1, "x");
        assertEquals(Arrays.asList("a", "x"), fs);
    }

    @Test
    public void fixedSizeList_listIteratorRemoveIsBlocked() {
        List<String> fs = FixedSizeList.fixedSizeList(arrayList("a", "b"));
        ListIterator<String> it = fs.listIterator();
        assertEquals("a", it.next());
        expectUOE(() -> it.remove());
    }

    @Test
    public void predicatedList_rejectsNullOnAdd() {
        List<String> pl = PredicatedList.predicatedList(new ArrayList<>(), PredicateUtils.notNullPredicate());
        expectIAE(() -> pl.add(null));
        assertTrue(pl.isEmpty());
    }

    @Test
    public void predicatedList_rejectsNullOnSet() {
        List<String> pl = PredicatedList.predicatedList(arrayList("a"), PredicateUtils.notNullPredicate());
        expectIAE(() -> pl.set(0, null));
        assertEquals("a", pl.get(0));
    }

    @Test
    public void predicatedList_addAllRejectsNullElement() {
        List<String> pl = PredicatedList.predicatedList(new ArrayList<>(), PredicateUtils.notNullPredicate());
        expectIAE(() -> pl.addAll(Arrays.asList("a", null, "b")));
        assertTrue(pl.isEmpty());
    }

    @Test
    public void predicatedList_allowsNonNullValues() {
        List<String> pl = PredicatedList.predicatedList(new ArrayList<>(), PredicateUtils.notNullPredicate());
        assertTrue(pl.add("a"));
        assertTrue(pl.add("b"));
        assertEquals(Arrays.asList("a", "b"), pl);
    }

    @Test
    public void setUniqueList_blocksDuplicateAdd() {
        List<String> sul = SetUniqueList.setUniqueList(new ArrayList<>());
        assertTrue(sul.add("a"));
        assertFalse(sul.add("a"));
        assertEquals(1, sul.size());
    }

    @Test
    public void setUniqueList_addAllKeepsOnlyUnique() {
        List<Integer> sul = SetUniqueList.setUniqueList(new ArrayList<>());
        sul.addAll(Arrays.asList(1, 2, 2, 3, 1));
        assertEquals(3, sul.size());
        assertTrue(sul.containsAll(Arrays.asList(1,2,3)));
    }

    @Test
    public void setUniqueList_allowsReorderingViaSet_whenUnique() {
        List<String> sul = SetUniqueList.setUniqueList(arrayList("a", "b", "c"));
        sul.set(0, "x");
        assertEquals(Arrays.asList("x", "b", "c"), sul);
    }

    @Test
    public void setUniqueList_setToExistingValue_maintainsUniqueness() {
        List<String> sul = SetUniqueList.setUniqueList(arrayList("a", "b", "c"));
        sul.set(0, "b");
        assertEquals(2, sul.stream().distinct().count());
    }

    @Test
    public void treeList_addAtIndex_insertsCorrectly() {
        TreeList<String> tl = new TreeList<>();
        tl.add("b");
        tl.add(0, "a");
        tl.add(2, "c");
        assertEquals(Arrays.asList("a","b","c"), tl);
    }

    @Test
    public void treeList_removeByIndex_removesCorrectElement() {
        TreeList<Integer> tl = new TreeList<>();
        tl.addAll(Arrays.asList(1,2,3));
        assertEquals(Integer.valueOf(2), tl.remove(1));
        assertEquals(Arrays.asList(1,3), tl);
    }

    @Test
    public void treeList_indexOf_findsElement() {
        TreeList<String> tl = new TreeList<>();
        tl.addAll(Arrays.asList("a","b","c"));
        assertEquals(1, tl.indexOf("b"));
        assertEquals(-1, tl.indexOf("x"));
    }

    @Test
    public void treeList_set_updatesElement() {
        TreeList<String> tl = new TreeList<>();
        tl.addAll(Arrays.asList("a","b"));
        assertEquals("b", tl.set(1, "x"));
        assertEquals(Arrays.asList("a","x"), tl);
    }

    @Test
    public void growthList_setBeyondEnd_growsListWithNulls() {
        GrowthList<String> gl = GrowthList.growthList(new ArrayList<>());
        gl.set(3, "x");
        assertEquals(4, gl.size());
        assertNull(gl.get(0));
        assertEquals("x", gl.get(3));
    }

    @Test
    public void growthList_setWithinRange_replacesValue() {
        GrowthList<Integer> gl = GrowthList.growthList(arrayList(1,2,3));
        gl.set(1, 9);
        assertEquals(Arrays.asList(1,9,3), gl);
    }

    @Test
    public void growthList_getOutOfBounds_stillThrows() {
        GrowthList<String> gl = GrowthList.growthList(new ArrayList<>());
        try {
            gl.get(0);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // ok
        }
    }

    @Test
    public void growthList_addBehavesLikeNormalList() {
        GrowthList<String> gl = GrowthList.growthList(new ArrayList<>());
        gl.add("a");
        gl.add("b");
        assertEquals(Arrays.asList("a","b"), gl);
    }

    @Test
    public void transformedList_transformingList_transformsOnAdd() {
        Transformer<String,String> up = s -> s == null ? null : s.toUpperCase();
        List<String> base = new ArrayList<>();
        List<String> tl = TransformedList.transformingList(base, up);
        tl.add("a");
        assertEquals("A", tl.get(0));
    }

    @Test
    public void transformedList_transformingList_transformsOnSet() {
        Transformer<String,String> up = s -> s == null ? null : s.toUpperCase();
        List<String> base = arrayList("a");
        List<String> tl = TransformedList.transformingList(base, up);
        tl.set(0, "b");
        assertEquals("B", tl.get(0));
    }

    @Test
    public void transformedList_transformedList_transformsExistingElements() {
        Transformer<String,String> up = s -> s == null ? null : s.toUpperCase();
        List<String> base = arrayList("a","b");
        List<String> tl = TransformedList.transformedList(base, up);
        assertEquals(Arrays.asList("A","B"), tl);
    }

    @Test
    public void transformedList_addAll_transformsAllIncoming() {
        Transformer<String,String> up = s -> s == null ? null : s.toUpperCase();
        List<String> base = new ArrayList<>();
        List<String> tl = TransformedList.transformingList(base, up);
        tl.addAll(Arrays.asList("a","b","c"));
        assertEquals(Arrays.asList("A","B","C"), tl);
    }

    @Test
    public void unmodifiableList_blocksAdd() {
        List<String> ul = UnmodifiableList.unmodifiableList(arrayList("a"));
        expectUOE(() -> ul.add("b"));
        assertEquals(1, ul.size());
    }

    @Test
    public void unmodifiableList_blocksSet() {
        List<String> ul = UnmodifiableList.unmodifiableList(arrayList("a"));
        expectUOE(() -> ul.set(0, "x"));
        assertEquals("a", ul.get(0));
    }

    @Test
    public void unmodifiableList_iteratorRemoveBlocked() {
        List<String> ul = UnmodifiableList.unmodifiableList(arrayList("a","b"));
        ListIterator<String> it = ul.listIterator();
        assertEquals("a", it.next());
        expectUOE(() -> it.remove());
    }

    @Test
    public void unmodifiableList_reflectsUnderlyingListChanges() {
        List<String> base = arrayList("a");
        List<String> ul = UnmodifiableList.unmodifiableList(base);
        base.add("b");
        assertEquals(2, ul.size());
        assertEquals("b", ul.get(1));
    }

}
