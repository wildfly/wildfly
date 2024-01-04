/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * Represents a group of SFSBs that must be serialized together.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface BeanGroup<K, V extends BeanInstance<K>> extends ImmutableBeanGroup<K, V> {

    /**
     * Adds the specified bean instance to this group.
     * @param instance a bean instance
     */
    void addBeanInstance(V instance);

    /**
     * Removes the bean instance with the specified identifier from this group.
     * @param id a bean instance identifier
     * @return the removed bean instance, or null, if no such bean instance existed
     */
    V removeBeanInstance(K id);
}
