/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<InfinispanTimerMetaDataKey<UUID>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new InfinispanTimerMetaDataKey<>(UUID.randomUUID()));
    }
}
