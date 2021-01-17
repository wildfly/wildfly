/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Generic tests for java.util.concurrent.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractConcurrentTestCase {
    private static final Map<Object, Object> BASIS = Stream.of(1, 2, 3, 4, 5).collect(Collectors.<Integer, Object, Object>toMap(i -> i, i -> Integer.toString(-i)));

    private final MarshallingTesterFactory factory;

    public AbstractConcurrentTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testConcurrentHashMap() throws IOException {
        MarshallingTester<ConcurrentHashMap<Object, Object>> tester = this.factory.createTester();
        tester.test(new ConcurrentHashMap<>(BASIS), AbstractConcurrentTestCase::assertMapEquals);
    }

    @Test
    public void testConcurrentHashSet() throws IOException {
        ConcurrentHashMap.KeySetView<Object, Boolean> keySetView = ConcurrentHashMap.newKeySet();
        keySetView.addAll(BASIS.keySet());
        MarshallingTester<ConcurrentHashMap.KeySetView<Object, Boolean>> tester = this.factory.createTester();
        tester.test(keySetView, AbstractConcurrentTestCase::assertCollectionEquals);
    }

    @Test
    public void testConcurrentLinkedDeque() throws IOException {
        MarshallingTester<ConcurrentLinkedDeque<Object>> tester = this.factory.createTester();
        tester.test(new ConcurrentLinkedDeque<>(BASIS.keySet()), AbstractConcurrentTestCase::assertCollectionEquals);
    }

    @Test
    public void testConcurrentLinkedQueue() throws IOException {
        MarshallingTester<ConcurrentLinkedQueue<Object>> tester = this.factory.createTester();
        tester.test(new ConcurrentLinkedQueue<>(BASIS.keySet()), AbstractConcurrentTestCase::assertCollectionEquals);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentSkipListMap() throws IOException {
        MarshallingTester<ConcurrentSkipListMap<Object, Object>> tester = this.factory.createTester();

        ConcurrentSkipListMap<Object, Object> map = new ConcurrentSkipListMap<>();
        map.putAll(BASIS);
        tester.test(map, AbstractConcurrentTestCase::assertMapEquals);

        map = new ConcurrentSkipListMap<>((Comparator<Object>) (Comparator<?>) Comparator.reverseOrder());
        map.putAll(BASIS);
        tester.test(map, AbstractConcurrentTestCase::assertMapEquals);

        map = new ConcurrentSkipListMap<>(new TestComparator<>());
        map.putAll(BASIS);
        tester.test(map, AbstractConcurrentTestCase::assertMapEquals);

        tester.test(new ConcurrentSkipListMap<>(BASIS), AbstractConcurrentTestCase::assertMapEquals);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentSkipListSet() throws IOException {
        MarshallingTester<ConcurrentSkipListSet<Object>> tester = this.factory.createTester();

        ConcurrentSkipListSet<Object> set = new ConcurrentSkipListSet<>();
        set.addAll(BASIS.keySet());
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);

        set = new ConcurrentSkipListSet<>((Comparator<Object>) (Comparator<?>) Comparator.reverseOrder());
        set.addAll(BASIS.keySet());
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);

        set = new ConcurrentSkipListSet<>(new TestComparator<>());
        set.addAll(BASIS.keySet());
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testCopyOnWriteArrayList() throws IOException {
        MarshallingTester<CopyOnWriteArrayList<Object>> tester = this.factory.createTester();
        tester.test(new CopyOnWriteArrayList<>(BASIS.keySet()), AbstractConcurrentTestCase::assertCollectionEquals);
    }

    @Test
    public void testCopyOnWriteArraySet() throws IOException {
        MarshallingTester<CopyOnWriteArraySet<Object>> tester = this.factory.createTester();
        tester.test(new CopyOnWriteArraySet<>(BASIS.keySet()), AbstractConcurrentTestCase::assertCollectionEquals);
    }

    @Test
    public void testTimeUnit() throws IOException {
        this.factory.createTester(TimeUnit.class).test();
    }

    static <T extends Map<?, ?>> void assertMapEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(expected.keySet().containsAll(actual.keySet()));
        for (Map.Entry<?, ?> entry : expected.entrySet()) {
            Assert.assertEquals(entry.getValue(), actual.get(entry.getKey()));
        }
    }

    static <T extends Collection<?>> void assertCollectionEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(expected.containsAll(actual));
    }
}
