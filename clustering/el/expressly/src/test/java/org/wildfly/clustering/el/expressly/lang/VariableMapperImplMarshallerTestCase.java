/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import org.glassfish.expressly.ValueExpressionLiteral;
import org.glassfish.expressly.lang.VariableMapperImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import jakarta.el.ValueExpression;

/**
 * Validates marshalling of a {@link VariableMapperImpl}.
 * @author Paul Ferraro
 */
public class VariableMapperImplMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<VariableMapperImpl> tester = factory.createTester(Assertions::assertNotSame);
        VariableMapperImpl mapper = new VariableMapperImpl();
        tester.accept(mapper);

        tester = factory.createTester(VariableMapperImplMarshallerTestCase::assertEquals);
        mapper.setVariable("foo", new ValueExpressionLiteral(Boolean.TRUE, Boolean.class));
        mapper.setVariable("bar", new ValueExpressionLiteral(Integer.valueOf(1), Integer.class));
        tester.accept(mapper);
    }

    static void assertEquals(VariableMapperImpl mapper1, VariableMapperImpl mapper2) {
        assertEquals(mapper1, mapper2, "foo");
        assertEquals(mapper1, mapper2, "bar");
    }

    static void assertEquals(VariableMapperImpl mapper1, VariableMapperImpl mapper2, String variable) {
        ValueExpression expression1 = mapper1.resolveVariable(variable);
        ValueExpression expression2 = mapper2.resolveVariable(variable);
        Assertions.assertNotNull(expression1);
        Assertions.assertNotNull(expression2);
        Assertions.assertEquals(expression1, expression2);
    }
}
