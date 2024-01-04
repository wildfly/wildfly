/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import java.io.IOException;
import java.util.Set;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.wildfly.clustering.ee.cache.function.CollectionFunction;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class AddressSetFunctionMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<CollectionFunction<Address, Set<Address>>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        Address address = new JGroupsAddress(UUID.randomUUID());
        tester.test(new AddressSetAddFunction(address));
        tester.test(new AddressSetRemoveFunction(address));
    }
}
