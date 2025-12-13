/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.eviction.EvictionConfiguration;

/**
 * Encapsulates the configuration of a {@link org.wildfly.clustering.ejb.bean.BeanManagerFactory} implementation.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface BeanManagerFactoryConfiguration<K, V extends BeanInstance<K>, M extends GroupMember> {
    BeanGroupManager<K, V> getBeanGroupManager();
    BeanConfiguration getBeanConfiguration();
    EvictionConfiguration getPassivationConfiguration();
    Group<M> getGroup();
}
