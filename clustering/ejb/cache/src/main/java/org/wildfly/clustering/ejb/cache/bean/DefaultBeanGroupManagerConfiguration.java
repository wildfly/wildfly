/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;

/**
 * Encapsulates the configuration of a {@link DefaultBeanGroupManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <C> the marshalled value context type
 */
public interface DefaultBeanGroupManagerConfiguration<K, V extends BeanInstance<K>, C> {
    Creator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>> getCreator();
    Remover<K> getRemover();
    MutatorFactory<K, MarshalledValue<Map<K, V>, C>> getMutatorFactory();
    CacheProperties getCacheProperties();
    MarshalledValueFactory<C> getMarshalledValueFactory();
}
