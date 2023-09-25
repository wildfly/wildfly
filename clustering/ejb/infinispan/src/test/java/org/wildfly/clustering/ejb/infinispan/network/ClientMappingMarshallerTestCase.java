/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

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
