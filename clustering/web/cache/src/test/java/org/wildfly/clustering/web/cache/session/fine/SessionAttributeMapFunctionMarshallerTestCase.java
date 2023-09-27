/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.UUID;

import org.wildfly.clustering.ee.cache.function.MapPutFunction;
import org.wildfly.clustering.ee.cache.function.MapRemoveFunction;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapFunctionMarshallerTestCase {

    @Test
    public void testMapPutFunction() throws IOException {
        Tester<MapPutFunction<String, UUID>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ConcurrentSessionAttributeMapPutFunction("foo", UUID.randomUUID()), SessionAttributeMapFunctionMarshallerTestCase::assertEquals);
        tester.test(new CopyOnWriteSessionAttributeMapPutFunction("foo", UUID.randomUUID()), SessionAttributeMapFunctionMarshallerTestCase::assertEquals);
    }

    @Test
    public void testMapRemoveFunction() throws IOException {
        Tester<MapRemoveFunction<String, UUID>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ConcurrentSessionAttributeMapRemoveFunction("foo"), SessionAttributeMapFunctionMarshallerTestCase::assertEquals);
        tester.test(new CopyOnWriteSessionAttributeMapRemoveFunction("foo"), SessionAttributeMapFunctionMarshallerTestCase::assertEquals);
    }

    static <K, V> void assertEquals(MapPutFunction<K, V> function1, MapPutFunction<K, V> function2) {
        Assert.assertEquals(function1.getOperand(), function2.getOperand());
    }

    static <K, V> void assertEquals(MapRemoveFunction<K, V> function1, MapRemoveFunction<K, V> function2) {
        Assert.assertEquals(function1.getOperand(), function2.getOperand());
    }
}
