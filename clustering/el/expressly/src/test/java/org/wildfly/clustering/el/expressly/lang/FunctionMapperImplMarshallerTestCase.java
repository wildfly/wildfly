/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
