/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import java.util.function.Supplier;

import org.wildfly.clustering.ejb.bean.BeanStatistics;
import org.wildfly.clustering.ejb.remote.AffinitySupport;
import org.wildfly.clustering.server.service.Service;

/**
 * A stateful session bean cache.
 * Any {@link StatefulSessionBean} retrieved from this cache *must* invoke either {@link StatefulSessionBean#close()}, {@link StatefulSessionBean#remove()}, or {@link StatefulSessionBean#discard()} when complete.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> extends Service, BeanStatistics, AffinitySupport<K>, AutoCloseable {
    ThreadLocal<Object> CURRENT_GROUP = new ThreadLocal<>();

    /**
     * Creates and caches a stateful bean using a generated identifier.
     * @return the newly created session bean
     */
    StatefulSessionBean<K, V> createStatefulSessionBean();

    /**
     * Returns the stateful bean with the specified identifier, or null if no such bean exists.
     * @return an existing stateful bean, or null if none was found
     */
    StatefulSessionBean<K, V> findStatefulSessionBean(K id);

    /**
     * Checks whether the supplied {@link Throwable} is remotable - meaning it can be safely sent to the client over the wire.
     */
    default boolean isRemotable(Throwable throwable) {
        return true;
    }

    /**
     * Returns the identifier factory of this cache.
     * @return an identifier factory
     */
    Supplier<K> getIdentifierFactory();

    @Override
    void close();
}
