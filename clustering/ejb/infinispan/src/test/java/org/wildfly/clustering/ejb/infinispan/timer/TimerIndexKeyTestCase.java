/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.cache.infinispan.embedded.persistence.FormatterTesterFactory;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class TimerIndexKeyTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class, FormatterTesterFactory.class })
    public void test(TesterFactory factory) throws NoSuchMethodException {
        Tester<InfinispanTimerIndexKey> tester = factory.createKeyTester();
        tester.accept(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("test"), 0)));
        tester.accept(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("ejbTimeout", Object.class), 0)));
    }

    void test() {
    }

    void ejbTimeout(Object timer) {
    }
}
