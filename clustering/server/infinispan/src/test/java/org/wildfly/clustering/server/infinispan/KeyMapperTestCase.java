/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.util.UUID;
import org.junit.Test;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;
import org.wildfly.clustering.server.infinispan.group.AddressableNode;
import org.wildfly.clustering.server.infinispan.group.LocalNode;

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
    }
}
