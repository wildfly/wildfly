/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.weld.ejb.SerializedStatefulSessionObject;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class SerializedStatefulSessionObjectMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<SerializedStatefulSessionObject> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new SerializedStatefulSessionObject(ServiceName.JBOSS.append("foo", "bar"), new UUIDSessionID(UUID.randomUUID()), Map.of(SerializedStatefulSessionObjectMarshallerTestCase.class, ServiceName.of("foo", "bar"))), SerializedStatefulSessionObjectMarshallerTestCase::assertEquals);
    }

    static void assertEquals(SerializedStatefulSessionObject object1, SerializedStatefulSessionObject object2) {
        Assert.assertEquals(object1.getComponentServiceName(), object2.getComponentServiceName());
        Assert.assertEquals(object1.getServiceNames(), object2.getServiceNames());
        Assert.assertEquals(object1.getSessionID(), object2.getSessionID());
    }
}
