/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.util;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import com.sun.faces.util.LRUMap;

/**
 * @author Paul Ferraro
 */
public class LRUMapMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<LRUMap<Object, Object>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        LRUMap<Object, Object> map = new LRUMap<>(10);
        tester.test(map);
        map.put(1, "1");
        map.put(2, "2");
        tester.test(map);
    }
}
