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

package org.wildfly.clustering.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.msc.service.ServiceName;
import org.jgroups.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.group.AddressableNode;
import org.wildfly.clustering.server.group.LocalNode;

/**
 * @author Paul Ferraro
 */
public class KeyMapperTestCase {
    @Test
    public void test() throws UnknownHostException {
        TwoWayKey2StringMapper mapper = new KeyMapper();
        Assert.assertTrue(mapper.isSupportedType(LocalNode.class));
        Assert.assertTrue(mapper.isSupportedType(AddressableNode.class));
        Assert.assertTrue(mapper.isSupportedType(ServiceName.class));

        Set<String> formatted = new HashSet<>();

        Node localNode = new LocalNode("cluster", "node");
        String mappedLocalNode = mapper.getStringMapping(localNode);
        Assert.assertEquals(localNode, mapper.getKeyMapping(mappedLocalNode));
        Assert.assertTrue(formatted.add(mappedLocalNode));

        Node addressableNode = new AddressableNode(UUID.randomUUID(), "node", new InetSocketAddress(InetAddress.getLocalHost(), 0));
        String mappedAddressableNode = mapper.getStringMapping(addressableNode);
        Assert.assertEquals(addressableNode, mapper.getKeyMapping(mappedAddressableNode));
        Assert.assertTrue(formatted.add(mappedAddressableNode));

        ServiceName serviceName = ServiceName.of("node");
        String mappedServiceName = mapper.getStringMapping(serviceName);
        Assert.assertEquals(serviceName, mapper.getKeyMapping(mappedServiceName));
        Assert.assertTrue(formatted.add(mappedServiceName));
    }
}
