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

package org.wildfly.clustering.server.provider;

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
