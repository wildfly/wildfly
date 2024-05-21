/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.cache.infinispan.CacheKey;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerIndexKey extends CacheKey<TimerIndex> implements org.wildfly.clustering.ejb.cache.timer.TimerIndexKey {

    public InfinispanTimerIndexKey(TimerIndex index) {
        super(index);
    }
}
