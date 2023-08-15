/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link InfinispanBeanCreationMetaDataKey} marshalling.
 * @author Paul Ferraro
 */
public class InfinispanBeanMetaDataKeyTestCase {

    @Test
    public void test() throws IOException {
        InfinispanBeanMetaDataKey<SessionID> key = new InfinispanBeanMetaDataKey<>(new UUIDSessionID(UUID.randomUUID()));

        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
    }
}
