/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * Manages immutable groups of beans.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface ImmutableBeanGroupManager<K, V extends BeanInstance<K>> {

    ImmutableBeanGroup<K, V> getImmutableBeanGroup(K id);
}
