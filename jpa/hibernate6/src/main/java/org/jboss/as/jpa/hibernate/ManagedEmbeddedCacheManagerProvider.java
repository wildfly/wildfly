/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.PersistenceSettings;
import org.infinispan.hibernate.cache.spi.EmbeddedCacheManagerProvider;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.impl.internal.Notification;
import org.kohsuke.MetaInfServices;

/**
 * Provides a managed {@link EmbeddedCacheManager} instance to Infinispan's region factory implementation.
 * @author Paul Ferraro
 */
@MetaInfServices(EmbeddedCacheManagerProvider.class)
public class ManagedEmbeddedCacheManagerProvider implements EmbeddedCacheManagerProvider {
    public static final String CACHE_CONTAINER = "hibernate.cache.infinispan.container";
    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";
    public static final String SHARED = "hibernate.cache.infinispan.shared";
    public static final String DEFAULT_SHARED = "true";
    public static final String STATISTICS = "hibernate.cache.infinispan.statistics";

    @Override
    public EmbeddedCacheManager getEmbeddedCacheManager(Properties properties) {

        Properties settings = new Properties();
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        settings.setProperty(HibernateSecondLevelCache.CONTAINER, container);

        if (!Boolean.parseBoolean(properties.getProperty(SHARED, DEFAULT_SHARED))) {
            HibernateSecondLevelCache.addSecondLevelCacheDependencies(properties, null);

            settings.setProperty(HibernateSecondLevelCache.CACHE_TYPE, HibernateSecondLevelCache.CACHE_PRIVATE);

            // Find a suitable service name to represent this session factory instance
            String name = properties.getProperty(PersistenceSettings.SESSION_FACTORY_NAME);
            if (name != null) {
                settings.setProperty(HibernateSecondLevelCache.NAME, name);
            }

            settings.setProperty(HibernateSecondLevelCache.CACHES, String.join(" ", HibernateSecondLevelCache.findCaches(properties)));
        }

        try {
            EmbeddedCacheManager manager = new JipiJapaCacheManager(Notification.startCache(Classification.INFINISPAN, settings));
            if (manager.getCacheManagerConfiguration().statistics()) {
                settings.setProperty(STATISTICS, Boolean.TRUE.toString());
            }
            return manager;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    private static class JipiJapaCacheManager extends AbstractDelegatingEmbeddedCacheManager {
        private final Wrapper wrapper;

        JipiJapaCacheManager(Wrapper wrapper) {
            super((EmbeddedCacheManager) wrapper.getValue());
            this.wrapper = wrapper;
        }

        @Override
        public void stop() {
            Notification.stopCache(Classification.INFINISPAN, this.wrapper);
        }

        @Override
        public void close() {
            this.stop();
        }
    }
}
