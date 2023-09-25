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
public class TimerAccessMetaDataKeyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<TimerAccessMetaDataKey<UUID>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new TimerAccessMetaDataKey<>(UUID.randomUUID()));
    }
}
