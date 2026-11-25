/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

/**
 * Creates a {@link BeanManager}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <B> the batch type
 */
public interface BeanManagerFactory<K, V extends BeanInstance<K>> {
    BeanManager<K, V> createBeanManager(BeanManagerConfiguration<K, V> configuration);
}
