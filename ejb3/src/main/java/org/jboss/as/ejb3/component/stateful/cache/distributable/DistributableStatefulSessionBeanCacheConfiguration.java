/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheConfiguration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;

/**
 * Configuration of a distributable stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface DistributableStatefulSessionBeanCacheConfiguration<K, V extends StatefulSessionBeanInstance<K>> extends StatefulSessionBeanCacheConfiguration<K, V> {

    BeanManager<K, V> getBeanManager();
}
