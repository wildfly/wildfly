/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Map;

import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataEntry;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataKey;
import org.wildfly.clustering.function.Predicate;

/**
 * Filters a cache for entries specific to a particular bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanMetaDataFilter<K, V> implements Predicate<Map.Entry<? super K, ? super V>> {
    private final String beanName;

    public InfinispanBeanMetaDataFilter(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public boolean test(Map.Entry<? super K, ? super V> entry) {
        if (entry.getKey() instanceof BeanMetaDataKey) {
            Object value = entry.getValue();
            if (value instanceof BeanMetaDataEntry) {
                BeanMetaDataEntry<?> metaData = (BeanMetaDataEntry<?>) value;
                return this.beanName.equals(metaData.getName());
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof InfinispanBeanMetaDataFilter)) return false;
        InfinispanBeanMetaDataFilter<?, ?> filter = (InfinispanBeanMetaDataFilter<?, ?>) object;
        return this.beanName.equals(filter.beanName);
    }

    @Override
    public int hashCode() {
        return this.beanName.hashCode();
    }

    @Override
    public String toString() {
        return this.beanName;
    }
}
