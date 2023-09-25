/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * Factory for creating {@link ImmutableBeanMetaData} instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean metadata value type
 */
public interface ImmutableBeanMetaDataFactory<K, V> extends Locator<K, V> {
    ImmutableBeanMetaData<K> createImmutableBeanMetaData(K id, V value);
}
