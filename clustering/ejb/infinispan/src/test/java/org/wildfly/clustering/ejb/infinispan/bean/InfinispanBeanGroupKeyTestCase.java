/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.cache.infinispan.embedded.persistence.FormatterTesterFactory;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link InfinispanBeanGroupKey} marshalling.
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupKeyTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class, FormatterTesterFactory.class })
    public void test(TesterFactory factory) {
        Tester<InfinispanBeanGroupKey<SessionID>> tester = factory.createKeyTester();
        tester.accept(new InfinispanBeanGroupKey<>(new UUIDSessionID(UUID.randomUUID())));
    }
}
