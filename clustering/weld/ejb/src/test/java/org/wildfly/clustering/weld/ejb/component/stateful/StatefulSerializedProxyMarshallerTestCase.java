/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb.component.stateful;

import java.util.UUID;

import org.jboss.as.ejb3.component.stateful.StatefulSerializedProxy;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class StatefulSerializedProxyMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<StatefulSerializedProxy> tester = factory.createTester(StatefulSerializedProxyMarshallerTestCase::assertEquals);
        tester.accept(new StatefulSerializedProxy("foo", new UUIDSessionID(UUID.randomUUID())));
    }

    static void assertEquals(StatefulSerializedProxy proxy1, StatefulSerializedProxy proxy2) {
        Assertions.assertEquals(proxy1.getSessionID(), proxy2.getSessionID());
        Assertions.assertEquals(proxy1.getViewName(), proxy2.getViewName());
    }
}
