/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean;

import org.jboss.weld.bean.StringBeanIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of {@link StringBeanIdentifier}.
 * @author Paul Ferraro
 */
public class StringBeanIdentifierMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<StringBeanIdentifier> tester = factory.createTester();
        tester.accept(new StringBeanIdentifier("foo"));
    }
}
