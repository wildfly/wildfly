/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.io.IOException;
import java.util.ServiceLoader;

import org.junit.Test;
import org.wildfly.clustering.el.MethodExpressionFactory;
import org.wildfly.clustering.faces.view.facelets.MockTagAttribute;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import com.sun.faces.facelets.el.TagMethodExpression;

/**
 * Validates marshalling of a {@link TagMethodExpression}.
 * @author Paul Ferraro
 */
public class TagMethodExpressionMarshallerTestCase {

    private final MethodExpressionFactory factory = ServiceLoader.load(MethodExpressionFactory.class, MethodExpressionFactory.class.getClassLoader()).iterator().next();

    @Test
    public void test() throws IOException {
        Tester<TagMethodExpression> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new TagMethodExpression(new MockTagAttribute("foo"), this.factory.createMethodExpression("foo", String.class, new Class[0])));
    }
}
