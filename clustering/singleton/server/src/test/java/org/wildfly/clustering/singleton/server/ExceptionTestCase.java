/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.io.IOException;

import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for marshalling of singleton service exceptions.
 * @author Paul Ferraro
 */
public class ExceptionTestCase {

    @Test
    public void test() throws IOException {
        Tester<Throwable> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new StartException(), ExceptionTestCase::assertEquals);
        tester.test(new StartException("message"), ExceptionTestCase::assertEquals);
        tester.test(new StartException(new Exception()), ExceptionTestCase::assertEquals);
        tester.test(new StartException("message", new Exception()), ExceptionTestCase::assertEquals);

        tester.test(new ServiceNotFoundException(), ExceptionTestCase::assertEquals);
        tester.test(new ServiceNotFoundException("message"), ExceptionTestCase::assertEquals);
        tester.test(new ServiceNotFoundException(new Exception()), ExceptionTestCase::assertEquals);
        tester.test(new ServiceNotFoundException("message", new Exception()), ExceptionTestCase::assertEquals);
    }

    private static void assertEquals(Throwable exception1, Throwable exception2) {
        if ((exception1 != null) && (exception2 != null)) {
            Assert.assertSame(exception1.getClass(), exception2.getClass());
            Assert.assertEquals(exception1.getMessage(), exception2.getMessage());
            assertEquals(exception1.getCause(), exception2.getCause());
        } else {
            Assert.assertSame(exception1, exception2);
        }
    }
}
