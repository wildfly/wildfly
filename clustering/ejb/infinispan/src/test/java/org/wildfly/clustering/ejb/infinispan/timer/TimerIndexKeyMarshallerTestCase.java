/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class TimerIndexKeyMarshallerTestCase {

    @Test
    public void test() throws IOException, NoSuchMethodException, SecurityException {
        Tester<InfinispanTimerIndexKey> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("test"), 0)));
        tester.test(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("ejbTimeout", Object.class), 0)));
    }

    void ejbTimeout(Object timer) {
    }
}
