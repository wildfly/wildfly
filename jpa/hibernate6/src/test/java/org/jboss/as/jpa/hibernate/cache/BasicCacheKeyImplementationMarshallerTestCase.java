/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate.cache;

import java.util.UUID;

import org.hibernate.cache.internal.BasicCacheKeyImplementation;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link BasicCacheKeyImplementationMarshaller}.
 * @author Paul Ferraro
 */
public class BasicCacheKeyImplementationMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<BasicCacheKeyImplementation> tester = factory.createKeyTester();
        UUID id = UUID.randomUUID();
        String entity = "foo";
        tester.accept(new BasicCacheKeyImplementation(id, entity, id.hashCode()));
        tester.accept(new BasicCacheKeyImplementation(id, entity, 1));
    }
}
