/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import java.io.IOException;

import org.glassfish.expressly.ValueExpressionLiteral;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link ValueExpressionLiteral}.
 * @author Paul Ferraro
 */
public class ValueExpressionLiteralMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<ValueExpressionLiteral> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ValueExpressionLiteral(Boolean.TRUE, Boolean.class));
    }
}
