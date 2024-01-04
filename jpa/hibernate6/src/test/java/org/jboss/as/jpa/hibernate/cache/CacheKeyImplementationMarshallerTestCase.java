/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate.cache;

import java.io.IOException;
import java.util.UUID;

import org.hibernate.cache.internal.CacheKeyImplementation;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link CacheKeyImplementationMarshaller}.
 * @author Paul Ferraro
 */
public class CacheKeyImplementationMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<CacheKeyImplementation> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        UUID id = UUID.randomUUID();
        String entity = "foo";
        String tenant = "bar";
        tester.testKey(new CacheKeyImplementation(id, entity, tenant, id.hashCode()));
        tester.testKey(new CacheKeyImplementation(id, entity, tenant, 1));
    }
}
