/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import java.util.function.Consumer;

/**
 * Described the mutable and immutable properties of a cached bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface Bean<K, V extends BeanInstance<K>> extends ImmutableBean<K, V>, AutoCloseable {

    /**
     * Returns the metadata of this bean.
     * @return
     */
    @Override
    BeanMetaData<K> getMetaData();

    /**
     * Indicates whether or not this bean is valid, i.e. not closed nor removed.
     * @return true, if this bean is valid, false otherwise
     */
    boolean isValid();

    /**
     * Removes this bean from the cache, executing the specified task.
     */
    void remove(Consumer<V> removeTask);

    /**
     * Closes any resources used by this bean.
     */
    @Override
    void close();
}
