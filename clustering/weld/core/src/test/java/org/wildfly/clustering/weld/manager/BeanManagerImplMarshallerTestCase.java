/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.manager;

import java.io.IOException;

import org.jboss.weld.manager.BeanManagerImpl;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link BeanManagerImpl}.
 * @author Paul Ferraro
 */
public class BeanManagerImplMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<BeanManagerImpl> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(BeanManagerProvider.INSTANCE.apply("foo", "bar"));
    }
}
