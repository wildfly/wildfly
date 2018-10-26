/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2018, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jipijapa.plugin.spi;

import javax.persistence.EntityManager;

/**
 * EntityManagerCache is an internal SPI, that allows application server non-jpa 'container components' (e.g. Weld JPA module) to have access to the EntityManagerCache.
 *
 * @author Scott Marlow
 */
public interface EntityManagerCache {

    /**
     * Obtain EntityManager from cache, returned entity manager will no longer be in the cache,
     * until it is released back to the cache.
     *
     * @return available EntityManager or null if none available.
     */
    EntityManager get();

    /**
     * Put the specified EntityManager in the cache.
     * @param entityManager
     */
    void put(EntityManager entityManager);

    /**
     * Clear the current cache contents, should only be called at application undeployment time.
     *
     * This method is not atomic, if it were called before undeploy time,
     * the current cache contents would be cleared but the internal state might not be consistent
     * (e.g. EntityManagerCacheGlobal.cache_size might not be correct).
     */
    void clear();
}
