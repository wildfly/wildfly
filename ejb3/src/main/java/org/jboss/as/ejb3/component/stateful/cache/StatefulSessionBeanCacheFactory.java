/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

/**
 * Factory for creating a stateful session bean cache for a component.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCacheFactory<K, V extends StatefulSessionBeanInstance<K>> {

    /**
     * Creates a stateful session bean cache for a given {@link jakarta.ejb.Stateful} EJB.
     * @param configuration configuration of a stateful bean cache
     * @return a cache for a given {@link jakarta.ejb.Stateful} EJB.
     */
    StatefulSessionBeanCache<K, V> createStatefulBeanCache(StatefulSessionBeanCacheConfiguration<K, V> configuration);
}
