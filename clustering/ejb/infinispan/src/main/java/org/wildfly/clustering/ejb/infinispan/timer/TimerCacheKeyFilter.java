/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.infinispan.util.function.SerializablePredicate;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey;

/**
 * @author Paul Ferraro
 */
public enum TimerCacheKeyFilter implements SerializablePredicate<Object> {
    META_DATA_KEY(TimerMetaDataKey.class);

    private final Class<?> keyClass;

    TimerCacheKeyFilter(Class<?> keyClass) {
        this.keyClass = keyClass;
    }

    @Override
    public boolean test(Object key) {
        return this.keyClass.isInstance(key);
    }
}
