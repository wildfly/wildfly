/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate.cache;

import java.io.IOException;
import java.util.UUID;

import org.hibernate.cache.internal.BasicCacheKeyImplementation;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link BasicCacheKeyImplementationMarshaller}.
 * @author Paul Ferraro
 */
public class BasicCacheKeyImplementationMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<BasicCacheKeyImplementation> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        UUID id = UUID.randomUUID();
        String entity = "foo";
        tester.testKey(new BasicCacheKeyImplementation(id, entity, id.hashCode()));
        tester.testKey(new BasicCacheKeyImplementation(id, entity, 1));
    }
}
