/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import java.io.IOException;

import org.glassfish.expressly.MethodExpressionImpl;
import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.glassfish.expressly.lang.VariableMapperImpl;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link MethodExpressionImpl}.
 * @author Paul Ferraro
 */
public class MethodExpressionImplMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<MethodExpressionImpl> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new MethodExpressionImpl("foo", null, new FunctionMapperImpl(), new VariableMapperImpl(), String.class, new Class[0]));
        tester.test(new MethodExpressionImpl("bar", null, new FunctionMapperImpl(), new VariableMapperImpl(), String.class, new Class[] { Boolean.class, Integer.class }));
    }
}
