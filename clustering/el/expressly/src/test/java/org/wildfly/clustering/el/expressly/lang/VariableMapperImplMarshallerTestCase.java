/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import java.io.IOException;

import org.glassfish.expressly.ValueExpressionLiteral;
import org.glassfish.expressly.lang.VariableMapperImpl;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import jakarta.el.ValueExpression;

/**
 * Validates marshalling of a {@link VariableMapperImpl}.
 * @author Paul Ferraro
 */
public class VariableMapperImplMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<VariableMapperImpl> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        VariableMapperImpl mapper = new VariableMapperImpl();
        tester.test(mapper, Assert::assertNotSame);
        mapper.setVariable("foo", new ValueExpressionLiteral(Boolean.TRUE, Boolean.class));
        mapper.setVariable("bar", new ValueExpressionLiteral(Integer.valueOf(1), Integer.class));
        tester.test(mapper, VariableMapperImplMarshallerTestCase::assertEquals);
    }

    static void assertEquals(VariableMapperImpl mapper1, VariableMapperImpl mapper2) {
        assertEquals(mapper1, mapper2, "foo");
        assertEquals(mapper1, mapper2, "bar");
    }

    static void assertEquals(VariableMapperImpl mapper1, VariableMapperImpl mapper2, String variable) {
        ValueExpression expression1 = mapper1.resolveVariable(variable);
        ValueExpression expression2 = mapper2.resolveVariable(variable);
        Assert.assertNotNull(expression1);
        Assert.assertNotNull(expression2);
        Assert.assertEquals(expression1, expression2);
    }
}
