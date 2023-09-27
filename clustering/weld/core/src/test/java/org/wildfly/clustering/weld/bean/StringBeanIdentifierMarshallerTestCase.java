/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean;

import java.io.IOException;

import org.jboss.weld.bean.StringBeanIdentifier;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of {@link StringBeanIdentifier}.
 * @author Paul Ferraro
 */
public class StringBeanIdentifierMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<StringBeanIdentifier> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new StringBeanIdentifier("foo"));
    }
}
