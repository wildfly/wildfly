/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * Manages mutable and immutable groups of beans.
 * @author Paul Ferraro
 * @param <K> the bean and bean group identifier type
 * @param <V> the bean instance type
 */
public interface BeanGroupManager<K, V extends BeanInstance<K>> extends ImmutableBeanGroupManager<K, V> {

    @Override
    default ImmutableBeanGroup<K, V> getImmutableBeanGroup(K id) {
        return this.getBeanGroup(id);
    }

    BeanGroup<K, V> getBeanGroup(K id);
}
