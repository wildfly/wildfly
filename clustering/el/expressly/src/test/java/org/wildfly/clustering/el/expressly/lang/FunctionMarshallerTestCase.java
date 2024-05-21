/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import org.glassfish.expressly.lang.FunctionMapperImpl.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of a {@link Function}.
 * @author Paul Ferraro
 */
public class FunctionMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) throws NoSuchMethodException {
        Tester<Function> tester = factory.createTester();
        tester.accept(new Function(null, "foo", this.getClass().getDeclaredMethod("test")));
        tester.accept(new Function("foo", "bar", this.getClass().getDeclaredMethod("test")));
    }

    void test() {
    }
}
