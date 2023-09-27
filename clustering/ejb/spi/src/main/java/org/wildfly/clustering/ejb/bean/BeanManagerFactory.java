/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import org.wildfly.clustering.ee.Batch;

/**
 * Creates a {@link BeanManager}.
 *
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <B> the batch type
 */
public interface BeanManagerFactory<K, V extends BeanInstance<K>, B extends Batch> {
    BeanManager<K, V, B> createBeanManager(BeanManagerConfiguration<K, V> configuration);
}
