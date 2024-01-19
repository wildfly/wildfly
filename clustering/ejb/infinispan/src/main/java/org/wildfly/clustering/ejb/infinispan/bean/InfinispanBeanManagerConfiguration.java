/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanManagerConfiguration;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * Encapsulates the configuration of an {@link InfinispanBeanManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public interface InfinispanBeanManagerConfiguration<K, V extends BeanInstance<K>, M> extends BeanManagerConfiguration<K, V, M>, InfinispanBeanMetaDataFactoryConfiguration {
    @Override Group<Address> getGroup();
    KeyAffinityServiceFactory getAffinityFactory();
    CommandDispatcherFactory getCommandDispatcherFactory();
}
