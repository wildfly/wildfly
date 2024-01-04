/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import java.io.IOException;

import org.glassfish.expressly.lang.FunctionMapperImpl.Function;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Validates marshalling of a {@link Function}.
 * @author Paul Ferraro
 */
public class FunctionMarshallerTestCase {

    @Test
    public void test() throws NoSuchMethodException, IOException {
        Tester<Function> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new Function(null, "foo", this.getClass().getMethod("test")));
        tester.test(new Function("foo", "bar", this.getClass().getMethod("test")));
    }
}
