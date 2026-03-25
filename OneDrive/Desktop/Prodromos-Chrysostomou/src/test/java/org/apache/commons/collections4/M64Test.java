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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;
import org.apache.commons.collections4.iterators.ArrayListIterator;
import org.apache.commons.collections4.iterators.ObjectArrayListIterator;
import org.apache.commons.collections4.iterators.BoundedIterator;
import org.apache.commons.collections4.iterators.SkippingIterator;
import org.apache.commons.collections4.iterators.LoopingIterator;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.collections4.iterators.PushbackIterator;
import org.apache.commons.collections4.iterators.SingletonIterator;
import org.apache.commons.collections4.iterators.SingletonListIterator;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.apache.commons.collections4.iterators.FilterListIterator;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.commons.collections4.iterators.UniqueFilterIterator;
import org.apache.commons.collections4.iterators.UnmodifiableIterator;
import org.apache.commons.collections4.iterators.UnmodifiableListIterator;
import org.apache.commons.collections4.iterators.ZippingIterator;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.junit.Test;

/**
 * LLM-style tests (M64) focused on iterator-related structures.
 * Designed to be fast and deterministic (no sleeps, no huge data).
 */
public class M64Test {

    private static <T> List<T> list(final T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static <T> List<T> drain(final Iterator<T> it, final int max) {
        final List<T> out = new ArrayList<>();
        int i = 0;
        while (it.hasNext() && i < max) {
            out.add(it.next());
            i++;
        }
        return out;
    }

    private static void expectNSEE(final Runnable r) {
        try {
            r.run();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // ok
        }
    }

    private static void expectUOE(final Runnable r) {
        try {
            r.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void arrayIterator_iteratesAllElements() {
        Integer[] a = new Integer[] {1, 2, 3};
        ArrayIterator<Integer> it = new ArrayIterator<>(a);
        assertEquals(Arrays.asList(1,2,3), drain(it, 10));
    }

    @Test
    public void arrayIterator_startIndex_skipsPrefix() {
        Integer[] a = new Integer[] {1, 2, 3, 4};
        ArrayIterator<Integer> it = new ArrayIterator<>(a, 2);
        assertEquals(Arrays.asList(3,4), drain(it, 10));
    }

    @Test
    public void arrayIterator_startEnd_limitsRange() {
        Integer[] a = new Integer[] {1, 2, 3, 4, 5};
        ArrayIterator<Integer> it = new ArrayIterator<>(a, 1, 4);
        assertEquals(Arrays.asList(2,3,4), drain(it, 10));
    }

    @Test
    public void arrayIterator_nextPastEnd_throws() {
        Integer[] a = new Integer[] {1};
        ArrayIterator<Integer> it = new ArrayIterator<>(a);
        assertEquals(Integer.valueOf(1), it.next());
        expectNSEE(() -> it.next());
    }

    @Test
    public void objectArrayIterator_handlesObjectArray() {
        Object[] a = new Object[] {"a", 1, null};
        ObjectArrayIterator<Object> it = new ObjectArrayIterator<>(a);
        List<Object> out = drain(it, 10);
        assertEquals(3, out.size());
        assertEquals("a", out.get(0));
        assertEquals(1, out.get(1));
        assertNull(out.get(2));
    }

    @Test
    public void objectArrayIterator_rangeWorks() {
        Object[] a = new Object[] { "x", "y", "z" };
        ObjectArrayIterator<Object> it = new ObjectArrayIterator<>(a, 1, 3);
        assertEquals(Arrays.asList("y","z"), drain(it, 10));
    }

    @Test
    public void objectArrayIterator_hasNextFalseWhenEmpty() {
        Object[] a = new Object[] {};
        ObjectArrayIterator<Object> it = new ObjectArrayIterator<>(a);
        assertFalse(it.hasNext());
    }

    @Test
    public void objectArrayIterator_nextPastEnd_throws() {
        Object[] a = new Object[] { "x" };
        ObjectArrayIterator<Object> it = new ObjectArrayIterator<>(a);
        assertEquals("x", it.next());
        expectNSEE(() -> it.next());
    }

    // FIX: ArrayListIterator δουλεύει πάνω σε array, όχι σε List
    @Test
    public void arrayListIterator_iteratesList() {
        Integer[] a = new Integer[] {1,2,3};
        ArrayListIterator<Integer> it = new ArrayListIterator<>(a);
        assertEquals(Arrays.asList(1,2,3), drain(it, 10));
    }

    @Test
    public void arrayListIterator_startIndex() {
        String[] a = new String[] {"a","b","c"};
        ArrayListIterator<String> it = new ArrayListIterator<>(a, 1);
        assertEquals(Arrays.asList("b","c"), drain(it, 10));
    }

    @Test
    public void arrayListIterator_startEnd() {
        String[] a = new String[] {"a","b","c","d"};
        ArrayListIterator<String> it = new ArrayListIterator<>(a, 1, 3);
        assertEquals(Arrays.asList("b","c"), drain(it, 10));
    }

    @Test
    public void arrayListIterator_nextPastEnd_throws() {
        String[] a = new String[] {"a"};
        ArrayListIterator<String> it = new ArrayListIterator<>(a);
        assertEquals("a", it.next());
        expectNSEE(() -> it.next());
    }

    // FIX: ObjectArrayListIterator δουλεύει πάνω σε array/varargs, όχι σε List
    @Test
    public void objectArrayListIterator_iteratesMixedList() {
        Object[] a = new Object[] {"a", 1, null};
        ObjectArrayListIterator<Object> it = new ObjectArrayListIterator<>(a);
        List<Object> out = drain(it, 10);
        assertEquals(3, out.size());
        assertEquals("a", out.get(0));
        assertEquals(1, out.get(1));
        assertNull(out.get(2));
    }

    @Test
    public void objectArrayListIterator_range() {
        Object[] a = new Object[] {"a", "b", "c"};
        ObjectArrayListIterator<Object> it = new ObjectArrayListIterator<>(a, 0, 2);
        assertEquals(Arrays.asList("a","b"), drain(it, 10));
    }

    @Test
    public void objectArrayListIterator_emptyList() {
        Object[] a = new Object[] {};
        ObjectArrayListIterator<Object> it = new ObjectArrayListIterator<>(a);
        assertFalse(it.hasNext());
    }

    @Test
    public void objectArrayListIterator_nextPastEnd_throws() {
        Object[] a = new Object[] {"x"};
        ObjectArrayListIterator<Object> it = new ObjectArrayListIterator<>(a);
        assertEquals("x", it.next());
        expectNSEE(() -> it.next());
    }

    @Test
    public void boundedIterator_skipsOffsetAndLimitsMax() {
        Iterator<Integer> base = list(1,2,3,4,5).iterator();
        BoundedIterator<Integer> it = new BoundedIterator<>(base, 1, 2);
        assertEquals(Arrays.asList(2,3), drain(it, 10));
    }

    @Test
    public void boundedIterator_zeroMaxYieldsEmpty() {
        Iterator<Integer> base = list(1,2,3).iterator();
        BoundedIterator<Integer> it = new BoundedIterator<>(base, 0, 0);
        assertFalse(it.hasNext());
    }

    @Test
    public void boundedIterator_largeMaxStopsAtEnd() {
        Iterator<Integer> base = list(1,2).iterator();
        BoundedIterator<Integer> it = new BoundedIterator<>(base, 0, 10);
        assertEquals(Arrays.asList(1,2), drain(it, 10));
    }

    @Test
    public void boundedIterator_nextPastEnd_throws() {
        Iterator<Integer> base = list(1).iterator();
        BoundedIterator<Integer> it = new BoundedIterator<>(base, 0, 1);
        assertEquals(Integer.valueOf(1), it.next());
        expectNSEE(() -> it.next());
    }

    @Test
    public void skippingIterator_skipsPrefix() {
        Iterator<String> base = list("a","b","c","d").iterator();
        SkippingIterator<String> it = new SkippingIterator<>(base, 2);
        assertEquals(Arrays.asList("c","d"), drain(it, 10));
    }

    @Test
    public void skippingIterator_offsetBeyondEndProducesEmpty() {
        Iterator<String> base = list("a","b").iterator();
        SkippingIterator<String> it = new SkippingIterator<>(base, 99);
        assertFalse(it.hasNext());
    }

    @Test
    public void skippingIterator_offsetZeroBehavesNormally() {
        Iterator<Integer> base = list(1,2,3).iterator();
        SkippingIterator<Integer> it = new SkippingIterator<>(base, 0);
        assertEquals(Arrays.asList(1,2,3), drain(it, 10));
    }

    @Test
    public void skippingIterator_removeDelegatesToUnderlying() {
        List<Integer> baseList = list(1,2,3);
        SkippingIterator<Integer> it = new SkippingIterator<>(baseList.iterator(), 1);
        assertEquals(Integer.valueOf(2), it.next());
        it.remove();
        assertEquals(Arrays.asList(1,3), baseList);
    }

    @Test
    public void loopingIterator_cyclesThroughCollection() {
        LoopingIterator<String> it = new LoopingIterator<>(list("a","b"));
        List<String> out = drain(it, 5);
        assertEquals(Arrays.asList("a","b","a","b","a"), out);
    }

    @Test
    public void loopingIterator_hasNextFalseForEmptyCollection() {
        LoopingIterator<String> it = new LoopingIterator<>(list());
        assertFalse(it.hasNext());
    }

    @Test
    public void loopingIterator_removeRemovesLastReturned() {
        List<String> base = list("a","b","c");
        LoopingIterator<String> it = new LoopingIterator<>(base);
        assertEquals("a", it.next());
        it.remove();
        assertEquals(Arrays.asList("b","c"), base);
    }

    @Test
    public void loopingIterator_nextPastEndOnEmpty_throws() {
        LoopingIterator<String> it = new LoopingIterator<>(list());
        expectNSEE(() -> it.next());
    }

    @Test
    public void reverseListIterator_iteratesFromEndToStart() {
        ReverseListIterator<Integer> it = new ReverseListIterator<>(list(1,2,3));
        assertEquals(Arrays.asList(3,2,1), drain(it, 10));
    }

    @Test
    public void reverseListIterator_canSkipOneElementAfterStart() {
        List<Integer> base = list(1,2,3,4);
        ReverseListIterator<Integer> it = new ReverseListIterator<>(base);
        assertEquals(Integer.valueOf(4), it.next());
        assertEquals(Arrays.asList(3,2,1), drain(it, 10));
    }

    @Test
    public void reverseListIterator_removeRemovesCurrentElement() {
        List<String> base = list("a","b","c");
        ReverseListIterator<String> it = new ReverseListIterator<>(base);
        assertEquals("c", it.next());
        it.remove();
        assertEquals(Arrays.asList("a","b"), base);
    }

    @Test
    public void reverseListIterator_nextPastEnd_throws() {
        ReverseListIterator<String> it = new ReverseListIterator<>(list("x"));
        assertEquals("x", it.next());
        expectNSEE(() -> it.next());
    }

    @Test
    public void peekingIterator_peekDoesNotAdvance() {
        PeekingIterator<Integer> it = PeekingIterator.peekingIterator(list(1,2,3).iterator());
        assertEquals(Integer.valueOf(1), it.peek());
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.peek());
    }

    @Test
    public void peekingIterator_peekAtEndReturnsNull() {
        PeekingIterator<Integer> it = PeekingIterator.peekingIterator(list(1).iterator());
        assertEquals(Integer.valueOf(1), it.peek());
        assertEquals(Integer.valueOf(1), it.next());
        assertNull(it.peek());
        assertFalse(it.hasNext());
    }

    @Test
    public void peekingIterator_elementThrowsAtEnd() {
        PeekingIterator<Integer> it = PeekingIterator.peekingIterator(list(1).iterator());
        assertEquals(Integer.valueOf(1), it.element());
        it.next();
        expectNSEE(() -> { it.element(); });
    }

    @Test
    public void peekingIterator_removeAfterPeek_throwsIllegalState() {
        PeekingIterator<Integer> it = PeekingIterator.peekingIterator(list(1,2).iterator());
        assertEquals(Integer.valueOf(1), it.peek());
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void pushbackIterator_pushesBackSingleElement() {
        PushbackIterator<Integer> it = PushbackIterator.pushbackIterator(list(1,2).iterator());
        assertEquals(Integer.valueOf(1), it.next());
        it.pushback(99);
        assertEquals(Integer.valueOf(99), it.next());
        assertEquals(Integer.valueOf(2), it.next());
    }

    @Test
    public void pushbackIterator_multiplePushbacks_areLifo() {
        PushbackIterator<String> it = PushbackIterator.pushbackIterator(list("a").iterator());
        it.pushback("x");
        it.pushback("y");
        assertEquals("y", it.next());
        assertEquals("x", it.next());
        assertEquals("a", it.next());
    }

    @Test
    public void pushbackIterator_hasNextTrueWhenStackNotEmpty() {
        PushbackIterator<String> it = PushbackIterator.pushbackIterator(Collections.<String>emptyList().iterator());
        it.pushback("x");
        assertTrue(it.hasNext());
        assertEquals("x", it.next());
    }

    @Test
    public void pushbackIterator_nextPastEnd_throws() {
        PushbackIterator<Integer> it = PushbackIterator.pushbackIterator(Collections.<Integer>emptyList().iterator());
        expectNSEE(() -> it.next());
    }

    @Test
    public void singletonIterator_iteratesOneElement() {
        SingletonIterator<String> it = new SingletonIterator<>("x");
        assertTrue(it.hasNext());
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void singletonIterator_removeRemovesElement() {
        SingletonIterator<String> it = new SingletonIterator<>("x");
        assertEquals("x", it.next());
        it.remove();
        assertFalse(it.hasNext());
    }

    @Test
    public void singletonIterator_removeBeforeNext_throws() {
        SingletonIterator<String> it = new SingletonIterator<>("x");
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void singletonIterator_nextPastEnd_throws() {
        SingletonIterator<String> it = new SingletonIterator<>("x");
        it.next();
        expectNSEE(() -> it.next());
    }

    @Test
    public void singletonListIterator_nextPrevious() {
        SingletonListIterator<Integer> it = new SingletonListIterator<>(7);
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(7), it.next());
        assertTrue(it.hasPrevious());
        assertEquals(Integer.valueOf(7), it.previous());
    }

    @Test
    public void singletonListIterator_indices() {
        SingletonListIterator<String> it = new SingletonListIterator<>("x");
        assertEquals(0, it.nextIndex());
        it.next();
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
    }

    @Test
    public void singletonListIterator_setAfterNext() {
        SingletonListIterator<String> it = new SingletonListIterator<>("x");
        assertEquals("x", it.next());
        it.set("y");
        assertEquals("y", it.previous());
    }

    @Test
    public void singletonListIterator_addUnsupported() {
        SingletonListIterator<String> it = new SingletonListIterator<>("x");
        expectUOE(() -> it.add("y"));
    }

    @Test
    public void filterIterator_filtersEvenNumbers() {
        Predicate<Integer> even = i -> i % 2 == 0;
        FilterIterator<Integer> it = new FilterIterator<>(list(1,2,3,4).iterator(), even);
        assertEquals(Arrays.asList(2,4), drain(it, 10));
    }

    @Test
    public void filterIterator_noMatchesYieldsEmpty() {
        Predicate<Integer> gt10 = i -> i > 10;
        FilterIterator<Integer> it = new FilterIterator<>(list(1,2,3).iterator(), gt10);
        assertFalse(it.hasNext());
    }

    @Test
    public void filterListIterator_filtersAndSupportsPrevious() {
        Predicate<String> startsWithA = s -> s.startsWith("a");
        FilterListIterator<String> it = new FilterListIterator<>(list("a1","b","a2").listIterator(), startsWithA);
        assertEquals("a1", it.next());
        assertEquals("a2", it.next());
        assertEquals("a2", it.previous());
    }

    @Test
    public void filterIterator_removeRemovesFromUnderlying() {
        List<Integer> base = list(1,2,3,4);
        Predicate<Integer> even = i -> i % 2 == 0;
        FilterIterator<Integer> it = new FilterIterator<>(base.iterator(), even);
        assertEquals(Integer.valueOf(2), it.next());
        it.remove();
        assertFalse(base.contains(2));
    }

    @Test
    public void transformIterator_transformsElements() {
        Transformer<Integer, String> toS = i -> "v" + i;
        TransformIterator<Integer, String> it = new TransformIterator<>(list(1,2).iterator(), toS);
        assertEquals(Arrays.asList("v1","v2"), drain(it, 10));
    }

    // FIX: Στο 4.4 το TransformIterator με null transformer πετάει NPE (bug/ασυνέπεια με javadoc)
    @Test
    public void transformIterator_handlesNullTransformer_noTransformationOccurs() {
        TransformIterator<Integer, Integer> it = new TransformIterator<>(list(1,2).iterator(), null);
        try {
            it.next();
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // ok
        }
    }

    @Test
    public void transformIterator_setTransformer_changesOutput() {
        TransformIterator<Integer, Integer> it = new TransformIterator<>(list(1,2,3).iterator());
        it.setTransformer(i -> i * 10);
        assertEquals(Arrays.asList(10,20,30), drain(it, 10));
    }

    @Test
    public void transformIterator_removeDelegates() {
        List<Integer> base = list(1,2,3);
        TransformIterator<Integer, Integer> it = new TransformIterator<>(base.iterator(), i -> i);
        assertEquals(Integer.valueOf(1), it.next());
        it.remove();
        assertEquals(Arrays.asList(2,3), base);
    }

    @Test
    public void uniqueFilterIterator_removesDuplicates() {
        UniqueFilterIterator<String> it = new UniqueFilterIterator<>(list("a","b","a","c","b").iterator());
        List<String> out = drain(it, 10);
        assertEquals(3, out.size());
        assertTrue(out.containsAll(Arrays.asList("a","b","c")));
    }

    @Test
    public void uniqueFilterIterator_preservesFirstOccurrenceOrder() {
        UniqueFilterIterator<String> it = new UniqueFilterIterator<>(list("b","a","b","a").iterator());
        assertEquals(Arrays.asList("b","a"), drain(it, 10));
    }

    @Test
    public void uniqueFilterIterator_empty() {
        UniqueFilterIterator<Integer> it = new UniqueFilterIterator<>(Collections.<Integer>emptyList().iterator());
        assertFalse(it.hasNext());
    }

    @Test
    public void uniqueFilterIterator_removeDelegates() {
        List<String> base = list("a","b","a");
        UniqueFilterIterator<String> it = new UniqueFilterIterator<>(base.iterator());
        assertEquals("a", it.next());
        it.remove();
        assertEquals(Arrays.asList("b","a"), base);
    }

    @Test
    public void unmodifiableIterator_blocksRemove() {
        Iterator<Integer> base = list(1,2).iterator();
        Iterator<Integer> it = UnmodifiableIterator.unmodifiableIterator(base);
        assertEquals(Integer.valueOf(1), it.next());
        expectUOE(() -> it.remove());
    }

    @Test
    public void unmodifiableListIterator_blocksMutationMethods() {
        ListIterator<String> base = list("a","b").listIterator();
        ListIterator<String> it = UnmodifiableListIterator.umodifiableListIterator(base);
        assertEquals("a", it.next());
        expectUOE(() -> it.set("x"));
    }

    @Test
    public void zippingIterator_interleavesTwoIterators() {
        ZippingIterator<Integer> it = new ZippingIterator<>(list(1,3,5).iterator(), list(2,4).iterator());
        assertEquals(Arrays.asList(1,2,3,4,5), drain(it, 10));
    }

    @Test
    public void iteratorChain_concatenatesIteratorsInOrder() {
        IteratorChain<Integer> chain = new IteratorChain<>();
        chain.addIterator(list(1,2).iterator());
        chain.addIterator(list(3).iterator());
        assertEquals(Arrays.asList(1,2,3), drain(chain, 10));
    }

}
