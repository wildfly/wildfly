/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate4.infinispan;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.jpa.hibernate4.HibernateSecondLevelCache;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Infinispan-backed region factory that retrieves its cache manager from the Infinispan subsystem.
 * This is used for (JPA) container managed persistence contexts.
 * Each deployment application will use a unique Hibernate cache region name in the shared cache.
 *
 * @author Paul Ferraro
 * @author Scott Marlow
 */
public class SharedInfinispanRegionFactory extends InfinispanRegionFactory {
    private static final long serialVersionUID = -3277051412715973863L;
    private volatile Wrapper wrapper;
    public SharedInfinispanRegionFactory() {
        super();
    }

    public SharedInfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) {
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        Properties cacheSettings = new Properties();
        cacheSettings.put(HibernateSecondLevelCache.CONTAINER, container);
        try {
            // Get the (shared) cache manager for JPA application use
            wrapper = Notification.startCache(Classification.INFINISPAN, cacheSettings);
            return (EmbeddedCacheManager)wrapper.getValue();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Do not attempt to stop our cache manager because it wasn't created by this region factory.
     * Base class stop() will call the base stopCacheRegions()
     */
    @Override
    protected void stopCacheManager() {
        // notify that the cache is not used but skip the stop since its shared for all jpa applications.
        Notification.stopCache(Classification.INFINISPAN, wrapper);
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
        cache.start();
        return cache;
    }
}
