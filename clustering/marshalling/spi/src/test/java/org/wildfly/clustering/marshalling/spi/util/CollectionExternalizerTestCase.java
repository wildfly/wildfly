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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import java.util.function.BiConsumer;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerTestUtil;
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
        test(DefaultExternalizer.ARRAY_DEQUE.cast(ArrayDeque.class), new ArrayDeque<>(basis));
        test(DefaultExternalizer.ARRAY_LIST.cast(ArrayList.class), new ArrayList<>(basis));
        test(DefaultExternalizer.CONCURRENT_LINKED_DEQUE.cast(ConcurrentLinkedDeque.class), new ConcurrentLinkedDeque<>(basis));
        test(DefaultExternalizer.CONCURRENT_LINKED_QUEUE.cast(ConcurrentLinkedQueue.class), new ConcurrentLinkedQueue<>(basis));
        test(DefaultExternalizer.HASH_SET.cast(HashSet.class), new HashSet<>(basis));
        test(DefaultExternalizer.LINKED_HASH_SET.cast(LinkedHashSet.class), new LinkedHashSet<>(basis));
        test(DefaultExternalizer.LINKED_LIST.cast(LinkedList.class), new LinkedList<>(basis));
        ConcurrentHashMap.KeySetView<Object, Boolean> keySetView = ConcurrentHashMap.newKeySet();
        keySetView.addAll(basis);
        test(DefaultExternalizer.CONCURRENT_HASH_SET.cast(ConcurrentHashMap.KeySetView.class), keySetView);

        test(DefaultExternalizer.COPY_ON_WRITE_ARRAY_LIST.cast(CopyOnWriteArrayList.class), new CopyOnWriteArrayList<>(basis));
        test(DefaultExternalizer.COPY_ON_WRITE_ARRAY_SET.cast(CopyOnWriteArraySet.class), new CopyOnWriteArraySet<>(basis));

        test(DefaultExternalizer.EMPTY_ENUMERATION.cast(Enumeration.class), Collections.emptyEnumeration());
        test(DefaultExternalizer.EMPTY_ITERATOR.cast(Iterator.class), Collections.emptyIterator());
        test(DefaultExternalizer.EMPTY_LIST.cast(List.class), Collections.emptyList());
        test(DefaultExternalizer.EMPTY_LIST_ITERATOR.cast(ListIterator.class), Collections.emptyListIterator());
        test(DefaultExternalizer.EMPTY_NAVIGABLE_SET.cast(NavigableSet.class), Collections.emptyNavigableSet());
        test(DefaultExternalizer.EMPTY_SET.cast(Set.class), Collections.emptySet());
        test(DefaultExternalizer.EMPTY_SORTED_SET.cast(SortedSet.class), Collections.emptySortedSet());

        test(DefaultExternalizer.SINGLETON_LIST.cast(List.class), Collections.singletonList(1));
        test(DefaultExternalizer.SINGLETON_SET.cast(Set.class), Collections.singleton(1));

        test(DefaultExternalizer.CONCURRENT_SKIP_LIST_SET.cast(ConcurrentSkipListSet.class), new ConcurrentSkipListSet<>(basis));
        test(DefaultExternalizer.TREE_SET.cast(TreeSet.class), new TreeSet<>(basis));
    }

    public static <T extends Collection<Object>> void test(Externalizer<T> externalizer, T collection) throws ClassNotFoundException, IOException {
        BiConsumer<T, T> assertSize = (expected, actual) -> assertEquals(expected.size(), actual.size());
        BiConsumer<T, T> assertContents = (expected, actual) -> assertTrue(actual.containsAll(expected));
        ExternalizerTestUtil.test(externalizer, collection, assertSize.andThen(assertContents));
    }

    public static <T extends Enumeration<Object>> void test(Externalizer<T> externalizer, T enumeration) throws ClassNotFoundException, IOException {
        ExternalizerTestUtil.test(externalizer, enumeration, (expected, actual) -> assertFalse(actual.hasMoreElements()));
    }

    public static <T extends Iterator<Object>> void test(Externalizer<T> externalizer, T iterator) throws ClassNotFoundException, IOException {
        ExternalizerTestUtil.test(externalizer, iterator, (expected, actual) -> assertFalse(actual.hasNext()));
    }
}
