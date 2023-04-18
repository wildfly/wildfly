/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.component.stateful.cache;

import java.util.function.Supplier;

import org.wildfly.clustering.ee.Restartable;
import org.wildfly.clustering.ejb.bean.BeanStatistics;
import org.wildfly.clustering.ejb.remote.AffinitySupport;

/**
 * A stateful session bean cache.
 * Any {@link StatefulSessionBean} retrieved from this cache *must* invoke either {@link StatefulSessionBean#close()}, {@link StatefulSessionBean#remove()}, or {@link StatefulSessionBean#discard()} when complete.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCache<K, V extends StatefulSessionBeanInstance<K>> extends Restartable, BeanStatistics, AffinitySupport<K> {
    ThreadLocal<Object> CURRENT_GROUP = new ThreadLocal<>();

    /**
     * Creates and caches a stateful bean using a generated identifier.
     * @return the identifier of the created session bean
     */
    K createStatefulSessionBean();

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
}
