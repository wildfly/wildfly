/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.web.el;

import java.util.ServiceLoader;

import org.jboss.weld.module.web.el.WeldValueExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.el.ValueExpressionFactory;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@WeldValueExpression}.
 * @author Paul Ferraro
 */
public class WeldValueExpressionMarshallerTestCase {

    private final ValueExpressionFactory factory = ServiceLoader.load(ValueExpressionFactory.class, ValueExpressionFactory.class.getClassLoader()).iterator().next();

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<WeldValueExpression> tester = factory.createTester(WeldValueExpressionMarshallerTestCase::assertEquals);

        tester.accept(new WeldValueExpression(this.factory.createValueExpression("foo", WeldValueExpressionMarshallerTestCase.class)));
    }

    static void assertEquals(WeldValueExpression expression1, WeldValueExpression expression2) {
        Assertions.assertEquals(expression1.getExpectedType(), expression2.getExpectedType());
        Assertions.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
    }
}
