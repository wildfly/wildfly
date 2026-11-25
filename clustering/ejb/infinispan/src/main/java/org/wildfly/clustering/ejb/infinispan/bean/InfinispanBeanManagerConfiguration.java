/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanManagerConfiguration;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;

/**
 * Encapsulates the configuration of an {@link InfinispanBeanManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public interface InfinispanBeanManagerConfiguration<K, V extends BeanInstance<K>, M> extends BeanManagerConfiguration<K, V, M, CacheContainerGroupMember>, InfinispanBeanMetaDataFactoryConfiguration {
    CacheContainerCommandDispatcherFactory getCommandDispatcherFactory();

    @Override
    default CacheContainerGroup getGroup() {
        return this.getCommandDispatcherFactory().getGroup();
    }
}
