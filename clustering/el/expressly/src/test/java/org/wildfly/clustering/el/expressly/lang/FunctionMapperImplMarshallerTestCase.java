/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import java.lang.reflect.Method;

import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link FunctionMapperImpl}.
 * @author Paul Ferraro
 */
public class FunctionMapperImplMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) throws NoSuchMethodException {
        Tester<FunctionMapperImpl> tester = factory.createTester(Assertions::assertNotSame);
        FunctionMapperImpl mapper = new FunctionMapperImpl();
        tester.accept(mapper);

        tester = factory.createTester(FunctionMapperImplMarshallerTestCase::assertEquals);
        mapper.addFunction(null, "foo", this.getClass().getDeclaredMethod("test"));
        mapper.addFunction("foo", "bar", this.getClass().getDeclaredMethod("test"));
        tester.accept(mapper);
    }

    void test() {
    }

    static void assertEquals(FunctionMapperImpl mapper1, FunctionMapperImpl mapper2) {
        assertEquals(mapper1, mapper2, null, "foo");
        assertEquals(mapper1, mapper2, "foo", "bar");
    }

    static void assertEquals(FunctionMapperImpl mapper1, FunctionMapperImpl mapper2, String prefix, String localName) {
        Method method1 = mapper1.resolveFunction(prefix, localName);
        Method method2 = mapper2.resolveFunction(prefix, localName);
        Assertions.assertNotNull(method1);
        Assertions.assertNotNull(method2);
        Assertions.assertEquals(method1, method2);
    }
}
