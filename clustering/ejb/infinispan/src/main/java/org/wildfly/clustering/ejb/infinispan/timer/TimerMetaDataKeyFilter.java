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
public enum TimerMetaDataKeyFilter implements SerializablePredicate<Object> {
    INSTANCE;

    @Override
    public boolean test(Object key) {
        return key instanceof TimerMetaDataKey;
    }
}
