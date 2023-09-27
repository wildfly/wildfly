/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.server.infinispan.group.LocalAddressSerializer.LocalAddressExternalizer;
import org.wildfly.clustering.server.infinispan.group.LocalAddressSerializer.LocalAddressFormatter;

/**
 * @author Paul Ferraro
 */
public class LocalAddressSerializerTestCase {

    @Test
    public void test() throws IOException {
        test(new ExternalizerTester<>(new LocalAddressExternalizer()));
        test(new FormatterTester<>(new LocalAddressFormatter()));
        test(JBossMarshallingTesterFactory.INSTANCE.createTester());
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    static void test(Tester<Address> tester) throws IOException {
        tester.test(LocalModeAddress.INSTANCE);
    }
}
