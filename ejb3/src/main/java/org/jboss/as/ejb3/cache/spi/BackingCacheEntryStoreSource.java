/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.spi;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * Provides {@link BackingCacheEntryStore} instances to a {@link CacheFactory} that needs to create a
 * {@link GroupAwareBackingCache}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public interface BackingCacheEntryStoreSource<K extends Serializable, V extends Cacheable<K>, G extends Serializable> extends BackingCacheEntryStoreConfig {
    /**
     * Provide a {@link BackingCacheEntryStore} for storage of serialization groups.
     *
     * @return the store
     */
    <E extends SerializationGroup<K, V, G>> BackingCacheEntryStore<G, Cacheable<G>, E> createGroupIntegratedObjectStore(PassivationManager<G, E> passivationManager, StatefulTimeoutInfo timeout);

    /**
     * Provide a {@link BackingCacheEntryStore} for storage of serialization group members.
     *
     * @return the store
     */
    <E extends SerializationGroupMember<K, V, G>> BackingCacheEntryStore<K, V, E> createIntegratedObjectStore(String beanName, PassivationManager<K, E> passivationManager, StatefulTimeoutInfo timeout);

    void addDependencies(ServiceTarget target, ServiceBuilder<?> builder);
}
