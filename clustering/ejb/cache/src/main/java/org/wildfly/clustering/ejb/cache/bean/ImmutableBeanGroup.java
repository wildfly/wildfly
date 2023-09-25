/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * Exposes the context of, and manages the lifecycle for, groups of beans.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface ImmutableBeanGroup<K, V extends BeanInstance<K>> extends AutoCloseable {

    /**
     * Returns the unique identifier of this bean group.
     * @return a unique identifier
     */
    K getId();

    /**
     * Indicates whether or not this bean group contains and bean instances.
     * @return true, if this bean group is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns the bean instance with the specified identifier.
     * @param id a bean instance identifier
     * @return the requested bean instance, or null, if no such bean instance exists.
     */
    V getBeanInstance(K id);

    /**
     * Indicates that the caller is finished with the bean group, and that it should close any resources.
     */
    @Override
    void close();
}
