/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb.component.stateful;

import java.io.IOException;
import java.util.UUID;

import org.jboss.as.ejb3.component.stateful.StatefulSerializedProxy;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class StatefulSerializedProxyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<StatefulSerializedProxy> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new StatefulSerializedProxy("foo", new UUIDSessionID(UUID.randomUUID())), StatefulSerializedProxyMarshallerTestCase::assertEquals);
    }

    static void assertEquals(StatefulSerializedProxy proxy1, StatefulSerializedProxy proxy2) {
        Assert.assertEquals(proxy1.getSessionID(), proxy2.getSessionID());
        Assert.assertEquals(proxy1.getViewName(), proxy2.getViewName());
    }
}
