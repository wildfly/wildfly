/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.io.IOException;
import java.util.ServiceLoader;

import jakarta.faces.view.Location;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.el.ValueExpressionFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import com.sun.faces.facelets.el.ContextualCompositeValueExpression;

/**
 * Validates marshalling of a {@link ContextualCompositeValueExpression}.
 * @author Paul Ferraro
 */
public class ContextualCompositeValueExpressionMarshallerTestCase {

    private final ValueExpressionFactory factory = ServiceLoader.load(ValueExpressionFactory.class, ValueExpressionFactory.class.getClassLoader()).iterator().next();

    @Test
    public void test() throws IOException {
        Tester<ContextualCompositeValueExpression> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ContextualCompositeValueExpression(new Location("/path", 1, 2), this.factory.createValueExpression("foo", String.class)), ContextualCompositeValueExpressionMarshallerTestCase::assertEquals);
    }

    // ContextualCompositeValueExpression.equals(...) impl is screwy
    static void assertEquals(ContextualCompositeValueExpression expression1, ContextualCompositeValueExpression expression2) {
        Assert.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
        Assert.assertEquals(expression1.isLiteralText(), expression2.isLiteralText());
    }
}
