/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link TimeoutDescriptorMarshaller}.
 * @author Paul Ferraro
 */
public class TimeoutDescriptorMarshallerTestCase {

    @Test
    public void test() throws NoSuchMethodException, IOException {
        Tester<TimeoutDescriptor> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new TimeoutDescriptor(this.getClass().getDeclaredMethod("ejbTimeout")));
        tester.test(new TimeoutDescriptor(this.getClass().getDeclaredMethod("ejbTimeout", Object.class)));
        tester.test(new TimeoutDescriptor(this.getClass().getDeclaredMethod("timeout")));
        tester.test(new TimeoutDescriptor(this.getClass().getDeclaredMethod("timeout", Object.class)));
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
