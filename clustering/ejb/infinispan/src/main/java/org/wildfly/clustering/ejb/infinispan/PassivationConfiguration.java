/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.util.concurrent.Executor;

import org.wildfly.clustering.ejb.BeanPassivationConfiguration;
import org.wildfly.clustering.ejb.PassivationListener;

/**
 * The passivation-related configuration of a bean manager.
 *
 * @author Paul Ferraro
 *
 * @param <T> the bean instance type
 */
public interface PassivationConfiguration<T> {
    /**
     * A listener to notify in the event of passivation and activation.
     * @return an event listener
     */
    PassivationListener<T> getPassivationListener();

    /**
     * Indicates whether the cache used by this bean manager uses a cache store and can evict beans.
     * If so, passivation events are only triggered when a bean is passivated.
     * @return true, if bean can be evicted, false otherwise.
     */
    boolean isEvictionAllowed();

    /**
     * Indicates whether the cache used by this bean manager will serialize a given bean on every request.
     * If so, passivation events will be triggered every request.
     * @return true, if this cache is persistent, false otherwise.
     */
    boolean isPersistent();

    BeanPassivationConfiguration getConfiguration();

    Executor getExecutor();
}
