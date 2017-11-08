/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate5.infinispan;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.jpa.hibernate5.HibernateSecondLevelCache;
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
    protected EmbeddedCacheManager createCacheManager(Properties properties, final ServiceRegistry serviceRegistry) throws CacheException {
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
