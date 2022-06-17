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
package org.wildfly.clustering.weld.web.el;

import java.io.IOException;
import java.util.ServiceLoader;

import org.jboss.weld.module.web.el.WeldMethodExpression;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.el.MethodExpressionFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link WeldMethodExpression}.
 * @author Paul Ferraro
 */
public class WeldMethodExpressionMarshallerTestCase {

    private final MethodExpressionFactory factory = ServiceLoader.load(MethodExpressionFactory.class, MethodExpressionFactory.class.getClassLoader()).iterator().next();

    @Test
    public void test() throws IOException {
        Tester<WeldMethodExpression> tester = ProtoStreamTesterFactory.INSTANCE.createTester();

        tester.test(new WeldMethodExpression(this.factory.createMethodExpression("foo", WeldMethodExpressionMarshallerTestCase.class, new Class<?>[0])), WeldMethodExpressionMarshallerTestCase::assertEquals);
    }

    static void assertEquals(WeldMethodExpression expression1, WeldMethodExpression expression2) {
        Assert.assertEquals(expression1.getExpressionString(), expression2.getExpressionString());
    }
}
