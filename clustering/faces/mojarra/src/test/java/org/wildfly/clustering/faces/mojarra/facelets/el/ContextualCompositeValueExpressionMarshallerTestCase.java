/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.el.ValueExpressionFactory;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import com.sun.faces.facelets.el.ContextualCompositeValueExpression;

import jakarta.faces.view.Location;

/**
 * Validates marshalling of a {@link ContextualCompositeValueExpression}.
 * @author Paul Ferraro
 */
public class ContextualCompositeValueExpressionMarshallerTestCase {

    private final ValueExpressionFactory factory = ServiceLoader.load(ValueExpressionFactory.class, ValueExpressionFactory.class.getClassLoader()).iterator().next();

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<ContextualCompositeValueExpression> tester = factory.createTester(ContextualCompositeValueExpressionMarshallerTestCase::assertEquals);
        tester.accept(new ContextualCompositeValueExpression(new Location("/path", 1, 2), this.factory.createValueExpression("foo", String.class)));
    }

    // ContextualCompositeValueExpression.equals(...) impl is screwy
    static void assertEquals(ContextualCompositeValueExpression expression1, ContextualCompositeValueExpression expression2) {
        Assertions.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
        Assertions.assertEquals(expression1.isLiteralText(), expression2.isLiteralText());
    }
}
