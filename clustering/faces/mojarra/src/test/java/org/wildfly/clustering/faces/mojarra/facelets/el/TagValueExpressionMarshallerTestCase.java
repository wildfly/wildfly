/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.util.ServiceLoader;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.el.ValueExpressionFactory;
import org.wildfly.clustering.faces.view.facelets.MockTagAttribute;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import com.sun.faces.facelets.el.TagValueExpression;

/**
 * Validates marshalling of a {@link TagValueExpression}.
 * @author Paul Ferraro
 */
public class TagValueExpressionMarshallerTestCase {

    private final ValueExpressionFactory factory = ServiceLoader.load(ValueExpressionFactory.class, ValueExpressionFactory.class.getClassLoader()).iterator().next();

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<TagValueExpression> tester = factory.createTester();
        tester.accept(new TagValueExpression(new MockTagAttribute("foo"), this.factory.createValueExpression("foo", String.class)));
    }
}
