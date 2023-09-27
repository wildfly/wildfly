/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import java.io.IOException;
import java.util.Set;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.cache.function.CollectionFunction;
import org.wildfly.clustering.ee.cache.function.SetAddFunction;
import org.wildfly.clustering.ee.cache.function.SetRemoveFunction;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class AddressSetFunctionMarshallerTestCase {

    @Test
    public void testSetAddFunction() throws IOException {
        Tester<SetAddFunction<Address>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ConcurrentAddressSetAddFunction(LocalModeAddress.INSTANCE), AddressSetFunctionMarshallerTestCase::assertEquals);
        tester.test(new CopyOnWriteAddressSetAddFunction(LocalModeAddress.INSTANCE), AddressSetFunctionMarshallerTestCase::assertEquals);
    }

    @Test
    public void testSetRemoveFunction() throws IOException {
        Tester<SetRemoveFunction<Address>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ConcurrentAddressSetRemoveFunction(LocalModeAddress.INSTANCE), AddressSetFunctionMarshallerTestCase::assertEquals);
        tester.test(new CopyOnWriteAddressSetRemoveFunction(LocalModeAddress.INSTANCE), AddressSetFunctionMarshallerTestCase::assertEquals);
    }

    private static <V> void assertEquals(CollectionFunction<V, Set<V>> function1, CollectionFunction<V, Set<V>> function2) {
        Assert.assertEquals(function1.getOperand(), function2.getOperand());
    }
}
