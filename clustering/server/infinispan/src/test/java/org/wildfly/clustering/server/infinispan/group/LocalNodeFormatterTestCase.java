/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.server.infinispan.group.LocalNodeFormatter.LocalNodeExternalizer;

/**
 * Unit test for {@link LocalNodeFormatter}.
 * @author Paul Ferraro
 */
public class LocalNodeFormatterTestCase {
    private final LocalNode localNode = new LocalNode("name");

    @Test
    public void test() throws IOException {
        this.test(new ExternalizerTester<>(new LocalNodeExternalizer()));
        this.test(new FormatterTester<>(new LocalNodeFormatter()));
        this.test(JBossMarshallingTesterFactory.INSTANCE.createTester());
        this.test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    private void test(Tester<LocalNode> tester) throws IOException {
        tester.test(this.localNode);
    }
}
