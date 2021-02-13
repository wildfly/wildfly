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

package org.wildfly.clustering.ejb.infinispan;

import java.net.InetAddress;

import org.jboss.as.network.ClientMapping;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link ClientMappingMarshaller}.
 * @author Paul Ferraro
 */
public class ClientMappingMarshallerTestCase {

    @Test
    public void test() throws Exception {
        Tester<ClientMapping> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ClientMapping(InetAddress.getByName("0.0.0.0"), 0, InetAddress.getLoopbackAddress().getHostName(), 8080), ClientMappingMarshallerTestCase::assertEquals);
        tester.test(new ClientMapping(InetAddress.getLocalHost(), 16, InetAddress.getLocalHost().getHostName(), Short.MAX_VALUE), ClientMappingMarshallerTestCase::assertEquals);
    }

    static void assertEquals(ClientMapping mapping1, ClientMapping mapping2) {
        Assert.assertEquals(mapping1.getSourceNetworkAddress(), mapping2.getSourceNetworkAddress());
        Assert.assertEquals(mapping1.getSourceNetworkMaskBits(), mapping2.getSourceNetworkMaskBits());
        Assert.assertEquals(mapping1.getDestinationAddress(), mapping2.getDestinationAddress());
        Assert.assertEquals(mapping1.getDestinationPort(), mapping2.getDestinationPort());
    }
}
