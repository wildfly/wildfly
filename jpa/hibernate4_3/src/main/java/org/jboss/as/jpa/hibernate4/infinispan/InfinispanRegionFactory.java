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
import org.hibernate.cfg.AvailableSettings;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.jpa.hibernate4.HibernateSecondLevelCache;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Infinispan-backed region factory for use with standalone (i.e. non-JPA) Hibernate applications.
 * @author Paul Ferraro
 * @author Scott Marlow
 */
public class InfinispanRegionFactory extends org.hibernate.cache.infinispan.InfinispanRegionFactory {
    private static final long serialVersionUID = 6526170943015350422L;

    public static final String CACHE_CONTAINER = "hibernate.cache.infinispan.container";
    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";
    public static final String CACHE_PRIVATE = "private";

    private volatile Wrapper wrapper;

    public InfinispanRegionFactory() {
        super();
    }

    public InfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
        // Find a suitable service name to represent this session factory instance
        String name = properties.getProperty(AvailableSettings.SESSION_FACTORY_NAME);
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        HibernateSecondLevelCache.addSecondLevelCacheDependencies(properties, null);

        Properties cacheSettings = new Properties();
        cacheSettings.setProperty(HibernateSecondLevelCache.CACHE_TYPE, CACHE_PRIVATE);
        cacheSettings.setProperty(HibernateSecondLevelCache.CONTAINER, container);
        if (name != null) {
            cacheSettings.setProperty(HibernateSecondLevelCache.NAME, name);
        }
        cacheSettings.setProperty(HibernateSecondLevelCache.CACHES, String.join(" ", HibernateSecondLevelCache.findCaches(properties)));

        try {
            // start a private cache for non-JPA use and return the started cache.
            wrapper = Notification.startCache(Classification.INFINISPAN, cacheSettings);
            return (EmbeddedCacheManager)wrapper.getValue();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    protected void stopCacheManager() {
        // stop the private cache
        Notification.stopCache(Classification.INFINISPAN, wrapper, false );
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
        return cache;
    }
}
