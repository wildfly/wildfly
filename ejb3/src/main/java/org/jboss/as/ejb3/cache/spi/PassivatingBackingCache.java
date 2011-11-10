/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.as.ejb3.cache.Cacheable;

/**
 * A {@link BackingCache} which passivates unused objects.
 * <p>
 * A PassivatingBackingCache is linked to a some sort of persistent store to store the passivated object and to a
 * PassivationManager to manage lifecycle callbacks on the object.
 * </p>
 *
 * @see BackingCacheEntryStore
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public interface PassivatingBackingCache<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>> extends BackingCache<K, V, E>, Passivatable<K> {
    /**
     * Gets any {@link GroupCompatibilityChecker} this cache is using.
     *
     * @return the checker, or <code>null</code> if this cache is not using one.
     */
    GroupCompatibilityChecker getCompatibilityChecker();
}
