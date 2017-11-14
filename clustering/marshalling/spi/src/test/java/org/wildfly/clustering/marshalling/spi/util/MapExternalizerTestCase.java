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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerTestUtil;
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
        test(DefaultExternalizer.CONCURRENT_HASH_MAP.cast(ConcurrentHashMap.class), new ConcurrentHashMap<>(basis));
        test(DefaultExternalizer.HASH_MAP.cast(HashMap.class), new HashMap<>(basis));
        test(DefaultExternalizer.LINKED_HASH_MAP.cast(LinkedHashMap.class), new LinkedHashMap<>(basis));

        test(DefaultExternalizer.EMPTY_MAP.cast(Map.class), Collections.emptyMap());
        test(DefaultExternalizer.EMPTY_NAVIGABLE_MAP.cast(NavigableMap.class), Collections.emptyNavigableMap());
        test(DefaultExternalizer.EMPTY_SORTED_MAP.cast(SortedMap.class), Collections.emptySortedMap());

        test(DefaultExternalizer.SINGLETON_MAP.cast(Map.class), Collections.singletonMap(1, 2));

        test(DefaultExternalizer.CONCURRENT_SKIP_LIST_MAP.cast(ConcurrentSkipListMap.class), new ConcurrentSkipListMap<>(basis));
        test(DefaultExternalizer.TREE_MAP.cast(TreeMap.class), new TreeMap<>(basis));
    }

    public static <T extends Map<Object, Object>> void test(Externalizer<T> externalizer, T collection) throws ClassNotFoundException, IOException {
        BiConsumer<T, T> assertSize = (expected, actual) -> assertEquals(expected.size(), actual.size());
        BiConsumer<T, T> assertContents = (expected, actual) -> assertTrue(actual.keySet().containsAll(expected.keySet()));
        ExternalizerTestUtil.test(externalizer, collection, assertSize.andThen(assertContents));
    }
}
