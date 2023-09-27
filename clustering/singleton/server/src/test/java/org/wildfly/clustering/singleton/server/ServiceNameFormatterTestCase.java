/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.io.IOException;

import org.jboss.msc.service.ServiceName;
import org.junit.Test;
import org.wildfly.clustering.infinispan.persistence.DynamicKeyFormatMapper;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.singleton.server.ServiceNameFormatter.ServiceNameExternalizer;

/**
 * Unit test for {@link ServiceNameFormatter}.
 * @author Paul Ferraro
 */
public class ServiceNameFormatterTestCase {
    private final ServiceName name = ServiceName.JBOSS.append("foo", "bar");

    @Test
    public void test() throws IOException {
        new ExternalizerTester<>(new ServiceNameExternalizer()).test(this.name);
        new FormatterTester<>(new ServiceNameFormatter()).test(this.name);
        new KeyMapperTester(new DynamicKeyFormatMapper(Thread.currentThread().getContextClassLoader())).test(this.name);

        JBossMarshallingTesterFactory.INSTANCE.createTester().test(this.name);
        ProtoStreamTesterFactory.INSTANCE.createTester().test(this.name);
    }
}
