/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanManagerFactoryConfiguration;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;

/**
 * Encapsulates the configuration for an {@link InfinispanBeanManagerFactory}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface InfinispanBeanManagerFactoryConfiguration<K, V extends BeanInstance<K>> extends BeanManagerFactoryConfiguration<K, V, CacheContainerGroupMember>, EmbeddedCacheConfiguration {
    @Override
    default CacheContainerGroup getGroup() {
        return this.getCommandDispatcherFactory().getGroup();
    }

    CacheContainerCommandDispatcherFactory getCommandDispatcherFactory();
}
