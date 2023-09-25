/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.server.infinispan.group.JGroupsAddressSerializer.JGroupsAddressExternalizer;
import org.wildfly.clustering.server.infinispan.group.JGroupsAddressSerializer.JGroupsAddressFormatter;

/**
 * @author Paul Ferraro
 */
public class JGroupsAddressSerializerTestCase {

    @Test
    public void test() throws IOException {
        JGroupsAddress address = new JGroupsAddress(UUID.randomUUID());

        new ExternalizerTester<>(new JGroupsAddressExternalizer()).test(address);
        new FormatterTester<>(new JGroupsAddressFormatter()).test(address);
    }
}
