/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate.cache;

import java.util.UUID;

import org.hibernate.cache.internal.NaturalIdCacheKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link NaturalIdCacheKeyMarshaller}.
 * @author Paul Ferraro
 */
public class NaturalIdCacheKeyMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<NaturalIdCacheKey> tester = factory.createKeyTester();
        UUID id = UUID.randomUUID();
        String entity = "foo";
        String tenant = "bar";
        tester.accept(new NaturalIdCacheKey(id, entity, tenant, id.hashCode()));
        tester.accept(new NaturalIdCacheKey(id, entity, tenant, 1));
    }
}
