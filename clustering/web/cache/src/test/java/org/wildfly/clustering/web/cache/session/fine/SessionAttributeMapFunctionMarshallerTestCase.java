/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
