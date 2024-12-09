/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Validates marshalling of timer cache stream filters.
 * @author Paul Ferraro
 */
public class TimerCacheStreamFilterTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class })
    public void test(TesterFactory factory) {
        factory.createTester(TimerCacheKeyFilter.class).run();
        factory.createTester(TimerCacheEntryFilter.class).run();
    }
}
