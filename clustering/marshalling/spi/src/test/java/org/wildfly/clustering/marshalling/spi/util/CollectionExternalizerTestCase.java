/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Unit test for {@link CollectionExternalizer} externalizers.
 * @author Paul Ferraro
 */
public class CollectionExternalizerTestCase {

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws ClassNotFoundException, IOException {
        Collection<Object> basis = Arrays.<Object>asList(1, 2, 3, 4, 5);

        new ExternalizerTester<>(DefaultExternalizer.ARRAY_DEQUE.cast(ArrayDeque.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new ArrayDeque<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.ARRAY_LIST.cast(ArrayList.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new ArrayList<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.CONCURRENT_LINKED_DEQUE.cast(ConcurrentLinkedDeque.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new ConcurrentLinkedDeque<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.CONCURRENT_LINKED_QUEUE.cast(ConcurrentLinkedQueue.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new ConcurrentLinkedQueue<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.HASH_SET.cast(HashSet.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new HashSet<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.LINKED_HASH_SET.cast(LinkedHashSet.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new LinkedHashSet<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.LINKED_LIST.cast(LinkedList.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new LinkedList<>(basis));

        ConcurrentHashMap.KeySetView<Object, Boolean> keySetView = ConcurrentHashMap.newKeySet();
        keySetView.addAll(basis);
        new ExternalizerTester<>(DefaultExternalizer.CONCURRENT_HASH_SET.cast(ConcurrentHashMap.KeySetView.class), CollectionExternalizerTestCase::assertCollectionEquals).test(keySetView);

        new ExternalizerTester<>(DefaultExternalizer.COPY_ON_WRITE_ARRAY_LIST.cast(CopyOnWriteArrayList.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new CopyOnWriteArrayList<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.COPY_ON_WRITE_ARRAY_SET.cast(CopyOnWriteArraySet.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new CopyOnWriteArraySet<>(basis));

        new ExternalizerTester<>(DefaultExternalizer.EMPTY_ENUMERATION.cast(Enumeration.class), Assert::assertSame).test(Collections.emptyEnumeration());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_ITERATOR.cast(Iterator.class), Assert::assertSame).test(Collections.emptyIterator());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_LIST.cast(List.class), Assert::assertSame).test(Collections.emptyList());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_LIST_ITERATOR.cast(ListIterator.class), Assert::assertSame).test(Collections.emptyListIterator());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_NAVIGABLE_SET.cast(NavigableSet.class), Assert::assertSame).test(Collections.emptyNavigableSet());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_SET.cast(Set.class), Assert::assertSame).test(Collections.emptySet());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_SORTED_SET.cast(SortedSet.class), Assert::assertSame).test(Collections.emptySortedSet());

        new ExternalizerTester<>(DefaultExternalizer.SINGLETON_LIST.cast(List.class), CollectionExternalizerTestCase::assertCollectionEquals).test(Collections.singletonList(1));
        new ExternalizerTester<>(DefaultExternalizer.SINGLETON_SET.cast(Set.class), CollectionExternalizerTestCase::assertCollectionEquals).test(Collections.singleton(1));

        new ExternalizerTester<>(DefaultExternalizer.CONCURRENT_SKIP_LIST_SET.cast(ConcurrentSkipListSet.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new ConcurrentSkipListSet<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.TREE_SET.cast(TreeSet.class), CollectionExternalizerTestCase::assertCollectionEquals).test(new TreeSet<>(basis));
    }

    static <T extends Collection<Object>> void assertCollectionEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(expected.containsAll(actual));
    }
}
