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

package org.jboss.as.jpa.container;


import java.util.Deque;
import java.util.LinkedList;

import javax.persistence.EntityManager;

import org.jipijapa.plugin.spi.EntityManagerCache;

/**
 * BoundedEntityManagerCacheImpl is a Global (per application deployment/per persistence unit cache.
 * BoundedEntityManagerCacheImpl is thread-safe.
 *
 * @author Scott Marlow
 */
public class BoundedEntityManagerCacheImpl implements EntityManagerCache {

    private final Deque<EntityManager> cache = new LinkedList<>();
    private int cache_size;
    private final int max_size;

    /**
     * @param max_size specifies the max number of entity managers that can be in cache.
     */
    public BoundedEntityManagerCacheImpl(int max_size) {
        this.max_size = max_size;
    }

    @Override
    public synchronized EntityManager get() {
        EntityManager result;
        do {
            if (null == (result = cache.poll())) {
                // return null if cache is empty
                return null;
            }
            cache_size--;   // decrement for each removed EntityManager
        } while (!result.isOpen());
        return result;
    }

    @Override
    public synchronized void put(EntityManager entityManager) {
        if (entityManager.isOpen()) {
            if (cache_size < max_size) {
                cache_size++;
                cache.push(entityManager);
            } else {
                entityManager.close();   // not enough room in cache, so close EntityManager
            }
        }

    }

    // NOTE: only expected to be called at application undeployment time.
    @Override
    public synchronized void clear() {
        cache.clear();
    }
}
