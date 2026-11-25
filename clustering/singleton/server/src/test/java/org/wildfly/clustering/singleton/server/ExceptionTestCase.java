/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Consumer;

import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for marshalling of singleton service exceptions.
 * @author Paul Ferraro
 */
public class ExceptionTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Consumer<Throwable> tester = factory.createTester(ExceptionTestCase::assertEquals);
        tester.accept(new StartException());
        tester.accept(new StartException("message"));
        tester.accept(new StartException(new Exception()));
        tester.accept(new StartException("message", new Exception()));

        tester.accept(new ServiceNotFoundException());
        tester.accept(new ServiceNotFoundException("message"));
        tester.accept(new ServiceNotFoundException(new Exception()));
        tester.accept(new ServiceNotFoundException("message", new Exception()));
    }

    private static void assertEquals(Throwable exception1, Throwable exception2) {
        if ((exception1 != null) && (exception2 != null)) {
            Assertions.assertSame(exception1.getClass(), exception2.getClass());
            Assertions.assertEquals(exception1.getMessage(), exception2.getMessage());
            assertEquals(exception1.getCause(), exception2.getCause());
        } else {
            Assertions.assertSame(exception1, exception2);
        }
    }
}
