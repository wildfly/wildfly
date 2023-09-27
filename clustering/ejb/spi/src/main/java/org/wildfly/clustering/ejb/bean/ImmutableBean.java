/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

/**
 * Describes the immutable properties of a cached bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface ImmutableBean<K, V extends BeanInstance<K>> {
    /**
     * Returns the identifier of this bean.
     * @return a unique identifier
     */
    K getId();

    /**
     * Returns the instance of this bean.
     * @return the bean instance
     */
    V getInstance();

    /**
     * Returns the metadata of this bean.
     * @return
     */
    ImmutableBeanMetaData<K> getMetaData();
}
