/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.web.el;

import java.io.IOException;
import java.util.ServiceLoader;

import org.jboss.weld.module.web.el.WeldMethodExpression;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.el.MethodExpressionFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link WeldMethodExpression}.
 * @author Paul Ferraro
 */
public class WeldMethodExpressionMarshallerTestCase {

    private final MethodExpressionFactory factory = ServiceLoader.load(MethodExpressionFactory.class, MethodExpressionFactory.class.getClassLoader()).iterator().next();

    @Test
    public void test() throws IOException {
        Tester<WeldMethodExpression> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(new WeldMethodExpression(this.factory.createMethodExpression("foo", WeldMethodExpressionMarshallerTestCase.class, new Class<?>[0])), WeldMethodExpressionMarshallerTestCase::assertEquals);
    }

    static void assertEquals(WeldMethodExpression expression1, WeldMethodExpression expression2) {
        Assert.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
    }
}
