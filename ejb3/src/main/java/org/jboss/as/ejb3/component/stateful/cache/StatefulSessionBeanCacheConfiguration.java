/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import java.time.Duration;

import org.wildfly.clustering.server.manager.ManagerConfiguration;

/**
 * Configuration of a stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCacheConfiguration<K, V extends StatefulSessionBeanInstance<K>> extends ManagerConfiguration<K> {

    String getComponentName();
    StatefulSessionBeanInstanceFactory<V> getInstanceFactory();
    Duration getTimeout();
}
