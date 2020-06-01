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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Unit test for {@link MapExternalizer} externalizers
 * @author Paul Ferraro
 */
public class MapExternalizerTestCase {

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws ClassNotFoundException, IOException {
        Map<Object, Object> basis = Stream.of(1, 2, 3, 4, 5).collect(Collectors.<Integer, Object, Object>toMap(i -> i, i -> Integer.toString(i)));
        new ExternalizerTester<>(DefaultExternalizer.CONCURRENT_HASH_MAP.cast(ConcurrentHashMap.class), MapExternalizerTestCase::assertMapEquals).test(new ConcurrentHashMap<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.HASH_MAP.cast(HashMap.class), MapExternalizerTestCase::assertMapEquals).test(new HashMap<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.LINKED_HASH_MAP.cast(LinkedHashMap.class), MapExternalizerTestCase::assertLinkedMapEquals).test(new LinkedHashMap<>(basis));
        LinkedHashMap<Object, Object> accessOrderMap = new LinkedHashMap<>(5, 1, true);
        accessOrderMap.putAll(basis);
        new ExternalizerTester<>(DefaultExternalizer.LINKED_HASH_MAP.cast(LinkedHashMap.class), MapExternalizerTestCase::assertLinkedMapEquals).test(accessOrderMap);

        new ExternalizerTester<>(DefaultExternalizer.EMPTY_MAP.cast(Map.class), Assert::assertSame).test(Collections.emptyMap());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_NAVIGABLE_MAP.cast(NavigableMap.class), Assert::assertSame).test(Collections.emptyNavigableMap());
        new ExternalizerTester<>(DefaultExternalizer.EMPTY_SORTED_MAP.cast(SortedMap.class), Assert::assertSame).test(Collections.emptySortedMap());

        EnumMap<TimeUnit, Integer> enumMap = new EnumMap<>(TimeUnit.class);
        new ExternalizerTester<>(DefaultExternalizer.ENUM_MAP.cast(EnumMap.class), MapExternalizerTestCase::assertMapEquals).test(enumMap);

        enumMap.put(TimeUnit.SECONDS, Integer.valueOf(1));
        enumMap.put(TimeUnit.MINUTES, Integer.valueOf(2));

        new ExternalizerTester<>(DefaultExternalizer.ENUM_MAP.cast(EnumMap.class), MapExternalizerTestCase::assertMapEquals).test(enumMap);
        new ExternalizerTester<>(DefaultExternalizer.SINGLETON_MAP.cast(Map.class), MapExternalizerTestCase::assertMapEquals).test(Collections.singletonMap(1, 2));

        new ExternalizerTester<>(DefaultExternalizer.CONCURRENT_SKIP_LIST_MAP.cast(ConcurrentSkipListMap.class), MapExternalizerTestCase::assertMapEquals).test(new ConcurrentSkipListMap<>(basis));
        new ExternalizerTester<>(DefaultExternalizer.TREE_MAP.cast(TreeMap.class), MapExternalizerTestCase::assertMapEquals).test(new TreeMap<>(basis));
    }

    static <T extends Map<Object, Object>> void assertMapEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(expected.keySet().containsAll(actual.keySet()));
        for (Map.Entry<Object, Object> entry : expected.entrySet()) {
            Assert.assertEquals(entry.getValue(), actual.get(entry.getKey()));
        }
    }

    static <T extends Map<Object, Object>> void assertLinkedMapEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        // Change access order
        expected.get(expected.keySet().iterator().next());
        actual.get(actual.keySet().iterator().next());
        Iterator<Map.Entry<Object, Object>> expectedEntries = expected.entrySet().iterator();
        Iterator<Map.Entry<Object, Object>> actualEntries = actual.entrySet().iterator();
        while (expectedEntries.hasNext() && actualEntries.hasNext()) {
            Map.Entry<Object, Object> expectedEntry = expectedEntries.next();
            Map.Entry<Object, Object> actualEntry = actualEntries.next();
            Assert.assertEquals(expectedEntry.getKey(), actualEntry.getKey());
            Assert.assertEquals(expectedEntry.getValue(), actualEntry.getValue());
        }
    }
}
