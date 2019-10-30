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

import org.jboss.msc.service.ServiceName;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.wildfly.clustering.infinispan.spi.persistence.KeyMapperTester;
import org.wildfly.clustering.server.group.AddressableNode;
import org.wildfly.clustering.server.group.LocalNode;

/**
 * Unit test for {@link KeyMapper}.
 * @author Paul Ferraro
 */
public class KeyMapperTestCase {
    @Test
    public void test() {
        KeyMapperTester tester = new KeyMapperTester(new KeyMapper());

        tester.test(new LocalNode("node"));
        tester.test(new AddressableNode(UUID.randomUUID(), "node", new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE)));
        tester.test(ServiceName.JBOSS.append("service"));
    }
}
