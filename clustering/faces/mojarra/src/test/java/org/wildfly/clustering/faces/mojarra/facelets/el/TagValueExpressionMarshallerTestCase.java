/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.io.IOException;
import java.util.ServiceLoader;

import org.junit.Test;
import org.wildfly.clustering.el.ValueExpressionFactory;
import org.wildfly.clustering.faces.view.facelets.MockTagAttribute;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

import com.sun.faces.facelets.el.TagValueExpression;

/**
 * Validates marshalling of a {@link TagValueExpression}.
 * @author Paul Ferraro
 */
public class TagValueExpressionMarshallerTestCase {

    private final ValueExpressionFactory factory = ServiceLoader.load(ValueExpressionFactory.class, ValueExpressionFactory.class.getClassLoader()).iterator().next();

    @Test
    public void test() throws IOException {
        Tester<TagValueExpression> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new TagValueExpression(new MockTagAttribute("foo"), this.factory.createValueExpression("foo", String.class)));
    }
}
