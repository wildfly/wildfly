/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.cache.infinispan.CacheKey;

/**
 * Encapsulates the meta data key for a timer
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public class InfinispanTimerMetaDataKey<I> extends CacheKey<I> implements org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey<I> {

    public InfinispanTimerMetaDataKey(I id) {
        super(id);
    }
}
