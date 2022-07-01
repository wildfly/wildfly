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
import java.lang.reflect.Method;

import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link FunctionMapperImpl}.
 * @author Paul Ferraro
 */
public class FunctionMapperImplMarshallerTestCase {

    @Test
    public void test() throws NoSuchMethodException, IOException {
        Tester<FunctionMapperImpl> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        FunctionMapperImpl mapper = new FunctionMapperImpl();
        tester.test(mapper, Assert::assertNotSame);
        mapper.addFunction(null, "foo", this.getClass().getMethod("test"));
        mapper.addFunction("foo", "bar", this.getClass().getMethod("test"));
        tester.test(mapper, FunctionMapperImplMarshallerTestCase::assertEquals);
    }

    static void assertEquals(FunctionMapperImpl mapper1, FunctionMapperImpl mapper2) {
        assertEquals(mapper1, mapper2, null, "foo");
        assertEquals(mapper1, mapper2, "foo", "bar");
    }

    static void assertEquals(FunctionMapperImpl mapper1, FunctionMapperImpl mapper2, String prefix, String localName) {
        Method method1 = mapper1.resolveFunction(prefix, localName);
        Method method2 = mapper2.resolveFunction(prefix, localName);
        Assert.assertNotNull(method1);
        Assert.assertNotNull(method2);
        Assert.assertEquals(method1, method2);
    }
}
