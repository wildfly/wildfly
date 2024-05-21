/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.manager;

import org.jboss.weld.manager.BeanManagerImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link BeanManagerImpl}.
 * @author Paul Ferraro
 */
public class BeanManagerImplMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<BeanManagerImpl> tester = factory.createTester();
        tester.accept(BeanManagerProvider.INSTANCE.apply("foo", "bar"));
    }
}
