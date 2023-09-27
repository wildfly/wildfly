/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import java.io.IOException;

import org.glassfish.expressly.MethodExpressionLiteral;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link MethodExpressionLiteral}.
 * @author Paul Ferraro
 */
public class MethodExpressionLiteralMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<MethodExpressionLiteral> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new MethodExpressionLiteral("foo", String.class, new Class[0]));
        tester.test(new MethodExpressionLiteral("bar", String.class, new Class[] { Boolean.class, Integer.class}));
    }
}
