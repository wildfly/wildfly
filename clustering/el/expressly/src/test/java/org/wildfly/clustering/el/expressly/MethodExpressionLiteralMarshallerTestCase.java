/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.glassfish.expressly.MethodExpressionLiteral;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link MethodExpressionLiteral}.
 * @author Paul Ferraro
 */
public class MethodExpressionLiteralMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<MethodExpressionLiteral> tester = factory.createTester();
        tester.accept(new MethodExpressionLiteral("foo", String.class, new Class[0]));
        tester.accept(new MethodExpressionLiteral("bar", String.class, new Class[] { Boolean.class, Integer.class}));
    }
}
