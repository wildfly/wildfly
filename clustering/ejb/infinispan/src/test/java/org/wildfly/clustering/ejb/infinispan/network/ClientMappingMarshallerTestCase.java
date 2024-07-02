/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.as.network.ClientMapping;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link ClientMappingMarshaller}.
 * @author Paul Ferraro
 */
public class ClientMappingMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) throws UnknownHostException {
        Tester<ClientMapping> tester = factory.createTester(ClientMappingMarshallerTestCase::assertEquals);
        tester.accept(new ClientMapping(InetAddress.getByName("0.0.0.0"), 0, InetAddress.getLoopbackAddress().getHostName(), 8080));
        tester.accept(new ClientMapping(InetAddress.getLocalHost(), 16, InetAddress.getLocalHost().getHostName(), Short.MAX_VALUE));
    }

    static void assertEquals(ClientMapping mapping1, ClientMapping mapping2) {
        Assertions.assertEquals(mapping1.getSourceNetworkAddress(), mapping2.getSourceNetworkAddress());
        Assertions.assertEquals(mapping1.getSourceNetworkMaskBits(), mapping2.getSourceNetworkMaskBits());
        Assertions.assertEquals(mapping1.getDestinationAddress(), mapping2.getDestinationAddress());
        Assertions.assertEquals(mapping1.getDestinationPort(), mapping2.getDestinationPort());
    }
}
