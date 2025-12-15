/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.util;

import java.util.List;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import com.sun.faces.util.LRUMap;

/**
 * @author Paul Ferraro
 */
public class LRUMapMarshallerTestCase {
    private final Random random = new Random();

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<LRUMap<Object, Object>> tester = factory.createOrderedMapTester();
        int capacity = 10;
        LRUMap<Object, Object> map = new LRUMap<>(capacity);
        tester.accept(map);
        // Fill to capacity
        for (int i = 0; i < capacity; ++i) {
            int value = this.random.nextInt();
            map.put(value, Integer.toString(value));
        }
        Assertions.assertThat(map).hasSize(capacity);
        LRUMap<Object, Object> copy = tester.apply(map);
        // Exceed capacity
        int value = this.random.nextInt();
        for (LRUMap<Object, Object> m : List.of(map, copy)) {
            m.put(value, Integer.toString(value));
            Assertions.assertThat(m).hasSize(capacity);
        }
        // Verify that both capacity and recency were preserved
        Assertions.assertThat(copy).hasSize(capacity).containsExactlyInAnyOrderEntriesOf(map);
    }
}
