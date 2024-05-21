/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.glassfish.expressly.ValueExpressionLiteral;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link ValueExpressionLiteral}.
 * @author Paul Ferraro
 */
public class ValueExpressionLiteralMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<ValueExpressionLiteral> tester = factory.createTester();
        tester.accept(new ValueExpressionLiteral(Boolean.TRUE, Boolean.class));
    }
}
