/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.glassfish.expressly.ValueExpressionImpl;
import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.glassfish.expressly.lang.VariableMapperImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link ValueExpressionImpl}.
 * @author Paul Ferraro
 */
public class ValueExpressionImplMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<ValueExpressionImpl> tester = factory.createTester();
        tester.accept(new ValueExpressionImpl("foo", null, new FunctionMapperImpl(), new VariableMapperImpl(), String.class));
    }
}
