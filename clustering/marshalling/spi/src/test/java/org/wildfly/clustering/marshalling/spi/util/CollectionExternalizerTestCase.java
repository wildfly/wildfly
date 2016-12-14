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

/**
 * Unit test for {@link CollectionExternalizer} externalizers.
 * @author Paul Ferraro
 */
public class CollectionExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        Collection<Object> basis = Arrays.<Object>asList(1, 2, 3, 4, 5);
        test(new CollectionExternalizer.ArrayDequeExternalizer(), new ArrayDeque<>(basis));
        test(new CollectionExternalizer.ArrayListExternalizer(), new ArrayList<>(basis));
        test(new CollectionExternalizer.ConcurrentLinkedDequeExternalizer(), new ConcurrentLinkedDeque<>(basis));
        test(new CollectionExternalizer.ConcurrentLinkedQueueExternalizer(), new ConcurrentLinkedQueue<>(basis));
        test(new CollectionExternalizer.HashSetExternalizer(), new HashSet<>(basis));
        test(new CollectionExternalizer.LinkedHashSetExternalizer(), new LinkedHashSet<>(basis));
        test(new CollectionExternalizer.LinkedListExternalizer(), new LinkedList<>(basis));
        ConcurrentHashMap.KeySetView<Object, Boolean> keySetView = ConcurrentHashMap.newKeySet();
        keySetView.addAll(basis);
        test(new CollectionExternalizer.ConcurrentHashSetExternalizer(), keySetView);

        test(new CopyOnWriteCollectionExternalizer.CopyOnWriteArrayListExternalizer(), new CopyOnWriteArrayList<>(basis));
        test(new CopyOnWriteCollectionExternalizer.CopyOnWriteArraySetExternalizer(), new CopyOnWriteArraySet<>(basis));

        test(new EmptyCollectionExternalizer.EmptyEnumerationExternalizer(), Collections.emptyEnumeration());
        test(new EmptyCollectionExternalizer.EmptyIteratorExternalizer(), Collections.emptyIterator());
        test(new EmptyCollectionExternalizer.EmptyListExternalizer(), Collections.emptyList());
        test(new EmptyCollectionExternalizer.EmptyListIteratorExternalizer(), Collections.emptyListIterator());
        test(new EmptyCollectionExternalizer.EmptyNavigableSetExternalizer(), Collections.emptyNavigableSet());
        test(new EmptyCollectionExternalizer.EmptySetExternalizer(), Collections.emptySet());
        test(new EmptyCollectionExternalizer.EmptySortedSetExternalizer(), Collections.emptySortedSet());

        test(new SingletonCollectionExternalizer.SingletonListExternalizer(), Collections.singletonList(1));
        test(new SingletonCollectionExternalizer.SingletonSetExternalizer(), Collections.singleton(1));

        test(new SortedSetExternalizer.ConcurrentSkipListSetExternalizer(), new ConcurrentSkipListSet<>(basis));
        test(new SortedSetExternalizer.TreeSetExternalizer(), new TreeSet<>(basis));
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
