/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.web.el;

import java.io.IOException;
import java.util.ServiceLoader;

import org.jboss.weld.module.web.el.WeldValueExpression;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.el.ValueExpressionFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@WeldValueExpression}.
 * @author Paul Ferraro
 */
public class WeldValueExpressionMarshallerTestCase {

    private final ValueExpressionFactory factory = ServiceLoader.load(ValueExpressionFactory.class, ValueExpressionFactory.class.getClassLoader()).iterator().next();

    @Test
    public void test() throws IOException {
        Tester<WeldValueExpression> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(new WeldValueExpression(this.factory.createValueExpression("foo", WeldValueExpressionMarshallerTestCase.class)), WeldValueExpressionMarshallerTestCase::assertEquals);
    }

    static void assertEquals(WeldValueExpression expression1, WeldValueExpression expression2) {
        Assert.assertEquals(expression1.getExpectedType(), expression2.getExpectedType());
        Assert.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
    }
}
