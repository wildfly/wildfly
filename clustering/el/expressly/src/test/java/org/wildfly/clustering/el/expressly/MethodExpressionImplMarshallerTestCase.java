/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.glassfish.expressly.MethodExpressionImpl;
import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.glassfish.expressly.lang.VariableMapperImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link MethodExpressionImpl}.
 * @author Paul Ferraro
 */
public class MethodExpressionImplMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<MethodExpressionImpl> tester = factory.createTester();
        tester.accept(new MethodExpressionImpl("foo", null, new FunctionMapperImpl(), new VariableMapperImpl(), String.class, new Class[0]));
        tester.accept(new MethodExpressionImpl("bar", null, new FunctionMapperImpl(), new VariableMapperImpl(), String.class, new Class[] { Boolean.class, Integer.class }));
    }
}
