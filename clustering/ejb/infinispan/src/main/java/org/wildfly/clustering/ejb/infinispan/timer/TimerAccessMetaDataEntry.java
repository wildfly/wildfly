/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.util.Map;

/**
 * @author Paul Ferraro
 */
public class TimerAccessMetaDataEntry<I> implements TimerAccessMetaData {

    private final Map<TimerAccessMetaDataKey<I>, Duration> cache;
    private final TimerAccessMetaDataKey<I> key;

    public TimerAccessMetaDataEntry(Map<TimerAccessMetaDataKey<I>, Duration> cache, TimerAccessMetaDataKey<I> key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public Duration getLastTimout() {
        return this.cache.get(this.key);
    }

    @Override
    public void setLastTimeout(Duration lastTimeout) {
        if (lastTimeout != null) {
            this.cache.put(this.key, lastTimeout);
        } else {
            this.cache.remove(this.key);
        }
    }
}
