/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.Map;

import org.wildfly.clustering.cache.CacheEntryCreator;
import org.wildfly.clustering.cache.CacheEntryMutatorFactory;
import org.wildfly.clustering.cache.CacheEntryRemover;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;

/**
 * Encapsulates the configuration of a {@link DefaultBeanGroupManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <C> the marshalled value context type
 */
public interface DefaultBeanGroupManagerConfiguration<K, V extends BeanInstance<K>, C> {
    CacheEntryCreator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>> getCreator();
    CacheEntryRemover<K> getRemover();
    CacheEntryMutatorFactory<K, MarshalledValue<Map<K, V>, C>> getMutatorFactory();
    CacheProperties getCacheProperties();
    MarshalledValueFactory<C> getMarshalledValueFactory();
}
