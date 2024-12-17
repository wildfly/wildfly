/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb;

import java.util.Map;
import java.util.UUID;

import org.jboss.as.weld.ejb.SerializedStatefulSessionObject;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.msc.service.ServiceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SerializedStatefulSessionObjectMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<SerializedStatefulSessionObject> tester = factory.createTester(SerializedStatefulSessionObjectMarshallerTestCase::assertEquals);
        tester.accept(new SerializedStatefulSessionObject(ServiceName.JBOSS.append("foo", "bar"), new UUIDSessionID(UUID.randomUUID()), Map.of(SerializedStatefulSessionObjectMarshallerTestCase.class, ServiceName.of("foo", "bar"))));
    }

    static void assertEquals(SerializedStatefulSessionObject object1, SerializedStatefulSessionObject object2) {
        Assertions.assertEquals(object1.getComponentServiceName(), object2.getComponentServiceName());
        Assertions.assertEquals(object1.getServiceNames(), object2.getServiceNames());
        Assertions.assertEquals(object1.getSessionID(), object2.getSessionID());
    }
}
