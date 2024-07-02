/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link TimeoutDescriptorMarshaller}.
 * @author Paul Ferraro
 */
public class TimeoutDescriptorMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) throws NoSuchMethodException {
        Tester<TimeoutDescriptor> tester = factory.createTester();
        tester.accept(new TimeoutDescriptor(this.getClass().getDeclaredMethod("ejbTimeout")));
        tester.accept(new TimeoutDescriptor(this.getClass().getDeclaredMethod("ejbTimeout", Object.class)));
        tester.accept(new TimeoutDescriptor(this.getClass().getDeclaredMethod("timeout")));
        tester.accept(new TimeoutDescriptor(this.getClass().getDeclaredMethod("timeout", Object.class)));
    }

    void timeout() {
        // Do nothing
    }

    void timeout(Object timer) {
        // Do nothing
    }

    void ejbTimeout() {
        // Do nothing
    }

    void ejbTimeout(Object timer) {
        // Do nothing
    }
}
