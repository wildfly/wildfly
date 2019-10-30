/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.group;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormatTester;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.server.group.AddressableNodeSerializer.AddressableNodeExternalizer;
import org.wildfly.clustering.server.group.AddressableNodeSerializer.AddressableNodeKeyFormat;

/**
 * Unit tests for {@link AddressableNodeSerializer}.
 * @author Paul Ferraro
 */
public class AddressableNodeSerializerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        AddressableNode node = new AddressableNode(UUID.randomUUID(), "node1", new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE));
        new ExternalizerTester<>(new AddressableNodeExternalizer(), AddressableNodeSerializerTestCase::assertEquals).test(node);
        new KeyFormatTester<>(new AddressableNodeKeyFormat(), AddressableNodeSerializerTestCase::assertEquals).test(node);
    }

    static void assertEquals(AddressableNode node1, AddressableNode node2) {
        Assert.assertEquals(node1, node2);
        Assert.assertEquals(node1.getName(), node2.getName());
        Assert.assertEquals(node1.getSocketAddress(), node2.getSocketAddress());
    }
}
