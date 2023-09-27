/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * Factory for creating {@link BeanMetaData} instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean metadata value
 */
public interface BeanMetaDataFactory<K, V> extends ImmutableBeanMetaDataFactory<K, V>, Creator<BeanInstance<K>, V, K>, Remover<K> {

    BeanMetaData<K> createBeanMetaData(K id, V value);
}
