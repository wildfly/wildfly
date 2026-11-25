/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.cache.CacheEntryCreator;
import org.wildfly.clustering.cache.CacheEntryRemover;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * Factory for creating {@link BeanMetaData} instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean metadata value
 */
public interface BeanMetaDataFactory<K, V> extends ImmutableBeanMetaDataFactory<K, V>, CacheEntryCreator<BeanInstance<K>, V, K>, CacheEntryRemover<K> {

    BeanMetaData<K> createBeanMetaData(K id, V value);
}
