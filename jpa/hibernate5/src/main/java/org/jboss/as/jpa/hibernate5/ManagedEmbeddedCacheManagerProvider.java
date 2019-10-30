/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate5;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AvailableSettings;
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

    @Override
    public EmbeddedCacheManager getEmbeddedCacheManager(Properties properties) {

        Properties settings = new Properties();
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        settings.setProperty(HibernateSecondLevelCache.CONTAINER, container);

        if (!Boolean.parseBoolean(properties.getProperty(SHARED, DEFAULT_SHARED))) {
            HibernateSecondLevelCache.addSecondLevelCacheDependencies(properties, null);

            settings.setProperty(HibernateSecondLevelCache.CACHE_TYPE, HibernateSecondLevelCache.CACHE_PRIVATE);

            // Find a suitable service name to represent this session factory instance
            String name = properties.getProperty(AvailableSettings.SESSION_FACTORY_NAME);
            if (name != null) {
                settings.setProperty(HibernateSecondLevelCache.NAME, name);
            }

            settings.setProperty(HibernateSecondLevelCache.CACHES, String.join(" ", HibernateSecondLevelCache.findCaches(properties)));
        }

        try {
            return new JipiJapaCacheManager(Notification.startCache(Classification.INFINISPAN, settings));
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
