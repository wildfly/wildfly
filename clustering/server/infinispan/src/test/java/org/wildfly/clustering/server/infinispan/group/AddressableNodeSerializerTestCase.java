/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.server.infinispan.group.AddressableNodeSerializer.AddressableNodeExternalizer;
import org.wildfly.clustering.server.infinispan.group.AddressableNodeSerializer.AddressableNodeFormatter;

/**
 * Unit tests for {@link AddressableNodeSerializer}.
 * @author Paul Ferraro
 */
public class AddressableNodeSerializerTestCase {

    private final AddressableNode node = new AddressableNode(UUID.randomUUID(), "foo", new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE));

    @Test
    public void test() throws IOException {
        this.test(new ExternalizerTester<>(new AddressableNodeExternalizer()));
        this.test(new FormatterTester<>(new AddressableNodeFormatter()));
        this.test(JBossMarshallingTesterFactory.INSTANCE.createTester());
        this.test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    public void test(Tester<AddressableNode> tester) throws IOException {
        tester.test(this.node, AddressableNodeSerializerTestCase::assertEquals);
    }

    static void assertEquals(AddressableNode expected, AddressableNode actual) {
        Assert.assertEquals(expected.getAddress(), actual.getAddress());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getSocketAddress(), actual.getSocketAddress());
    }
}
