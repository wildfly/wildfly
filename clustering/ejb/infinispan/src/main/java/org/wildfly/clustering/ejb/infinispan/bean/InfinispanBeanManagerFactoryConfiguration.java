/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanManagerFactoryConfiguration;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * Encapsulates the configuration for an {@link InfinispanBeanManagerFactory}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface InfinispanBeanManagerFactoryConfiguration<K, V extends BeanInstance<K>> extends BeanManagerFactoryConfiguration<K, V>, InfinispanConfiguration {
    KeyAffinityServiceFactory getKeyAffinityServiceFactory();
    @Override Group<Address> getGroup();
    CommandDispatcherFactory getCommandDispatcherFactory();
}
