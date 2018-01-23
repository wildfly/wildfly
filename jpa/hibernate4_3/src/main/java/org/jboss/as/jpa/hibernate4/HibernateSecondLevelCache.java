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

package org.jboss.as.jpa.hibernate4;

import static org.hibernate.cache.infinispan.InfinispanRegionFactory.COLLECTION_CACHE_RESOURCE_PROP;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.DEF_ENTITY_RESOURCE;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.DEF_QUERY_RESOURCE;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.NATURAL_ID_CACHE_RESOURCE_PROP;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.TIMESTAMPS_CACHE_RESOURCE_PROP;
import static org.jboss.as.jpa.hibernate4.infinispan.InfinispanRegionFactory.CACHE_CONTAINER;
import static org.jboss.as.jpa.hibernate4.infinispan.InfinispanRegionFactory.DEFAULT_CACHE_CONTAINER;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate4.infinispan.SharedInfinispanRegionFactory;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Second level cache setup.
 *
 * @author Scott Marlow
 */
public class HibernateSecondLevelCache {

    private static final String DEFAULT_REGION_FACTORY = SharedInfinispanRegionFactory.class.getName();

    public static final String CACHE_TYPE = "cachetype";    // shared (jpa) or private (for native applications)
    public static final String CONTAINER = "container";
    public static final String NAME = "name";
    public static final String CACHES = "caches";

    public static void addSecondLevelCacheDependencies(Properties mutableProperties, String scopedPersistenceUnitName) {

        if (mutableProperties.getProperty(AvailableSettings.CACHE_REGION_PREFIX) == null) {
            // cache entries for this PU will be identified by scoped pu name + Entity class name

            if (scopedPersistenceUnitName != null) {
                mutableProperties.setProperty(AvailableSettings.CACHE_REGION_PREFIX, scopedPersistenceUnitName);
            }
        }
        String regionFactory = mutableProperties.getProperty(AvailableSettings.CACHE_REGION_FACTORY);
        if (regionFactory == null) {
            regionFactory = DEFAULT_REGION_FACTORY;
            mutableProperties.setProperty(AvailableSettings.CACHE_REGION_FACTORY, regionFactory);
        }
        if (regionFactory.equals(DEFAULT_REGION_FACTORY)) {
            // Set infinispan defaults
            String container = mutableProperties.getProperty(CACHE_CONTAINER);
            if (container == null) {
                container = DEFAULT_CACHE_CONTAINER;
                mutableProperties.setProperty(CACHE_CONTAINER, container);
            }

            /**
             * AS will need the ServiceBuilder<?> builder that used to be passed to PersistenceProviderAdaptor.addProviderDependencies
             */
            Properties cacheSettings = new Properties();
            cacheSettings.setProperty(CONTAINER, container);
            cacheSettings.setProperty(CACHES, String.join(" ", findCaches(mutableProperties)));

            Notification.addCacheDependencies(Classification.INFINISPAN, cacheSettings);
        }
    }

    public static Set<String> findCaches(Properties properties) {
        Set<String> caches = new HashSet<>();

        caches.add(properties.getProperty(ENTITY_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(COLLECTION_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(NATURAL_ID_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        if (Boolean.parseBoolean(properties.getProperty(AvailableSettings.USE_QUERY_CACHE))) {
            caches.add(properties.getProperty(QUERY_CACHE_RESOURCE_PROP, DEF_QUERY_RESOURCE));
            caches.add(properties.getProperty(TIMESTAMPS_CACHE_RESOURCE_PROP, DEF_QUERY_RESOURCE));
        }

        int length = INFINISPAN_CONFIG_RESOURCE_PROP.length();
        String customRegionPrefix = INFINISPAN_CONFIG_RESOURCE_PROP.substring(0, length - 3) + properties.getProperty(AvailableSettings.CACHE_REGION_PREFIX, "");
        String customRegionSuffix = INFINISPAN_CONFIG_RESOURCE_PROP.substring(length - 4, length);

        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(customRegionPrefix) && propertyName.endsWith(customRegionSuffix)) {
                caches.add(properties.getProperty(propertyName));
            }
        }

        return caches;
    }
}
