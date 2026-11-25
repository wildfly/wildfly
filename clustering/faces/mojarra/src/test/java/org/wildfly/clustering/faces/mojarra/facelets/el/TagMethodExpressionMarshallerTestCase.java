/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.util.ServiceLoader;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.el.MethodExpressionFactory;
import org.wildfly.clustering.faces.view.facelets.MockTagAttribute;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

import com.sun.faces.facelets.el.TagMethodExpression;

/**
 * Validates marshalling of a {@link TagMethodExpression}.
 * @author Paul Ferraro
 */
public class TagMethodExpressionMarshallerTestCase {

    private final MethodExpressionFactory factory = ServiceLoader.load(MethodExpressionFactory.class, MethodExpressionFactory.class.getClassLoader()).iterator().next();

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<TagMethodExpression> tester = factory.createTester();
        tester.accept(new TagMethodExpression(new MockTagAttribute("foo"), this.factory.createMethodExpression("foo", String.class, new Class[0])));
    }
}
