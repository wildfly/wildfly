/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.ejb.component.session;

import java.io.IOException;

import org.jboss.as.ejb3.component.session.StatelessSerializedProxy;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class StatelessSerializedProxyMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<StatelessSerializedProxy> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new StatelessSerializedProxy("foo"), StatelessSerializedProxyMarshallerTestCase::assertEquals);
    }

    static void assertEquals(StatelessSerializedProxy proxy1, StatelessSerializedProxy proxy2) {
        Assert.assertEquals(proxy1.getViewName(), proxy2.getViewName());
    }
}
