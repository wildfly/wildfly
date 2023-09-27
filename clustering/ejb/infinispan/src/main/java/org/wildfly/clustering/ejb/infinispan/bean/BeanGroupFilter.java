/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.EventType;

/**
 * Filters a cache for entries specific to a particular bean.
 * @author Paul Ferraro
 */
public enum BeanGroupFilter implements CacheEventFilter<Object, Object> {
    INSTANCE;

    @Override
    public boolean accept(Object key, Object oldValue, Metadata oldMetadata, Object newValue, Metadata newMetadata, EventType eventType) {
        return key instanceof InfinispanBeanGroupKey;
    }
}
