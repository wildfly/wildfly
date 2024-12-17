/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.util;

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

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<LRUMap<Object, Object>> tester = factory.createTester();
        LRUMap<Object, Object> map = new LRUMap<>(10);
        tester.accept(map);
        map.put(1, "1");
        map.put(2, "2");
        tester.accept(map);
    }
}
