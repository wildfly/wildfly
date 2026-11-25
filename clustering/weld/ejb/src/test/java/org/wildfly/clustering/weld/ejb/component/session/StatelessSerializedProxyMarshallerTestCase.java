/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb.component.session;

import org.jboss.as.ejb3.component.session.StatelessSerializedProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class StatelessSerializedProxyMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<StatelessSerializedProxy> tester = factory.createTester(StatelessSerializedProxyMarshallerTestCase::assertEquals);
        tester.accept(new StatelessSerializedProxy("foo"));
    }

    static void assertEquals(StatelessSerializedProxy proxy1, StatelessSerializedProxy proxy2) {
        Assertions.assertEquals(proxy1.getViewName(), proxy2.getViewName());
    }
}
