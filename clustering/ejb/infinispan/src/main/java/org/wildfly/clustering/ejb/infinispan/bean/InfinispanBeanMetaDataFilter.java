/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Map;

import org.infinispan.util.function.SerializablePredicate;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataEntry;

/**
 * Filters a cache for entries specific to a particular bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanMetaDataFilter<K> implements SerializablePredicate<Map.Entry<? super Key<K>, ? super Object>> {
    private static final long serialVersionUID = -1079989480899595045L;

    private final String beanName;

    public InfinispanBeanMetaDataFilter(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public boolean test(Map.Entry<? super Key<K>, ? super Object> entry) {
        if (entry.getKey() instanceof InfinispanBeanMetaDataKey) {
            Object value = entry.getValue();
            if (value instanceof BeanMetaDataEntry) {
                @SuppressWarnings("unchecked")
                BeanMetaDataEntry<K> metaData = (BeanMetaDataEntry<K>) value;
                return this.beanName.equals(metaData.getName());
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.beanName;
    }
}
