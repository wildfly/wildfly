/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.cache;

import java.io.Serializable;

import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;

/**
 * Defines the contract for an EJB3 Stateful Cache Factory
 *
 * @author <a href="mailto:andrew.rubinger@redhat.com">ALR</a>
 * @author Brian Stansberry
 */
public interface CacheFactory<K extends Serializable, T extends Identifiable<K>> {
    /**
     * Creates a cache for a container.
     *
     * @param factory factory for creating objects managed by the cache
     * @param passivationManager manager for invoking pre and post passivation and replication callbacks on the cached objects
     * @param timeout the stateful timeout
     *
     * @return the cache
     */
    Cache<K, T> createCache(String beanName, StatefulObjectFactory<T> factory, PassivationManager<K, T> passivationManager, StatefulTimeoutInfo timeout);
}
