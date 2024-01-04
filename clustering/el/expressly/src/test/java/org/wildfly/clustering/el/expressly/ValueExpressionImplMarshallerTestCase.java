/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import java.io.IOException;

import org.glassfish.expressly.ValueExpressionImpl;
import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.glassfish.expressly.lang.VariableMapperImpl;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link ValueExpressionImpl}.
 * @author Paul Ferraro
 */
public class ValueExpressionImplMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<ValueExpressionImpl> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ValueExpressionImpl("foo", null, new FunctionMapperImpl(), new VariableMapperImpl(), String.class));
    }
}
