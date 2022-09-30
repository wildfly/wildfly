/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
