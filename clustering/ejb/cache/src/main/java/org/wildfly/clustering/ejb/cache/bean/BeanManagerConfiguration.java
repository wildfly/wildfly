/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.group.Group;

/**
 * Encapsulates the configuration of a {@link org.wildfly.clustering.ejb.bean.BeanManager} implementation.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface BeanManagerConfiguration<K, V extends BeanInstance<K>, M> extends  org.wildfly.clustering.ejb.bean.BeanManagerConfiguration<K, V>, BeanMetaDataFactoryConfiguration {
    BeanFactory<K, V, M> getBeanFactory();
    Group getGroup();
}
