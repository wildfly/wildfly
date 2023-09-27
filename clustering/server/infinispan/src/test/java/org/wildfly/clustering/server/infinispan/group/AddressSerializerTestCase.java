/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.server.infinispan.group.AddressSerializer.IpAddressExternalizer;
import org.wildfly.clustering.server.infinispan.group.AddressSerializer.UUIDExternalizer;

/**
 * @author Paul Ferraro
 */
public class AddressSerializerTestCase {

    @Test
    public void test() throws IOException {
        test(new ExternalizerTesterFactory(new UUIDExternalizer(), new IpAddressExternalizer()).createTester());
        test(JBossMarshallingTesterFactory.INSTANCE.createTester());
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    private static void test(Tester<Address> tester) throws IOException {
        UUID uuid = UUID.randomUUID();
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE);
        IpAddress ipAddress = new IpAddress(address);

        tester.test(uuid);
        tester.test(ipAddress);
    }
}
