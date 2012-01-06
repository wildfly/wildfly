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

import org.jboss.ejb.client.Affinity;

import java.io.Serializable;

/**
 * Cache a stateful object and make sure any life cycle callbacks are
 * called at the appropriate time.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface Cache<K extends Serializable, V extends Identifiable<K>> extends Removable<K> {
    /**
     * Creates and caches a new instance of <code>T</code>.
     *
     * @return a new <code>T</code>
     */
    V create();

    /**
     * Discard the specified object from cache.
     *
     * @param key the identifier of the object
     */
    void discard(K key);

    /**
     * Get the specified object from cache. This will mark
     * the object as being in use.
     *
     * @param key the identifier of the object
     * @return the object, or null if it does not exist
     */
    V get(K key);

    /**
     * Release the object from use.
     *
     * @param obj the object
     */
    void release(V obj);

    /**
     * Returns the strict affinity associated with the entries of this cache
     */
    Affinity getStrictAffinity();

    /**
     * Returns the weak affinity associated with the entries of this cache
     * @return
     */
    Affinity getWeakAffinity();


    /**
     * Start the cache.
     */
    void start();

    /**
     * Stop the cache.
     */
    void stop();
}
