/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.web.el;

import java.util.ServiceLoader;

import org.jboss.weld.module.web.el.WeldMethodExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.el.MethodExpressionFactory;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link WeldMethodExpression}.
 * @author Paul Ferraro
 */
public class WeldMethodExpressionMarshallerTestCase {

    private final MethodExpressionFactory factory = ServiceLoader.load(MethodExpressionFactory.class, MethodExpressionFactory.class.getClassLoader()).iterator().next();

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<WeldMethodExpression> tester = factory.createTester(WeldMethodExpressionMarshallerTestCase::assertEquals);

        tester.accept(new WeldMethodExpression(this.factory.createMethodExpression("foo", WeldMethodExpressionMarshallerTestCase.class, new Class<?>[0])));
    }

    static void assertEquals(WeldMethodExpression expression1, WeldMethodExpression expression2) {
        Assertions.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
    }
}
