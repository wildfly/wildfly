/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
