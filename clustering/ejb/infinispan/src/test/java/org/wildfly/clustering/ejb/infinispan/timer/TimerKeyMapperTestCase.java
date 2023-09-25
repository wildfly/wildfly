/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.ejb.infinispan.KeyMapper;
import org.wildfly.clustering.infinispan.persistence.KeyMapperTester;

/**
 * Validates {@link org.infinispan.persistence.keymappers.TwoWayKey2StringMapper} implementations for keys used by distributed timer service.
 * @author Paul Ferraro
 */
public class TimerKeyMapperTestCase {
    @Test
    public void test() throws NoSuchMethodException, SecurityException {
        KeyMapperTester tester = new KeyMapperTester(new KeyMapper());

        UUID id = UUID.randomUUID();
        tester.test(new InfinispanTimerMetaDataKey<>(id));
        tester.test(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("test"), 0)));
        tester.test(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("test"), 1)));
        tester.test(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("ejbTimeout", Object.class), 0)));
        tester.test(new InfinispanTimerIndexKey(new TimerIndex(this.getClass().getDeclaredMethod("ejbTimeout", Object.class), 2)));
    }

    void ejbTimeout(Object timer) {
    }
}
