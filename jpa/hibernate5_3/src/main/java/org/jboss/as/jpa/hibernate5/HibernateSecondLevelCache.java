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

package org.jboss.as.jpa.hibernate5;

import static org.infinispan.hibernate.cache.spi.InfinispanProperties.COLLECTION_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.DEF_ENTITY_RESOURCE;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.DEF_QUERY_RESOURCE;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.ENTITY_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.INFINISPAN_CONFIG_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.NATURAL_ID_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.PENDING_PUTS_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.QUERY_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.spi.InfinispanProperties.TIMESTAMPS_CACHE_RESOURCE_PROP;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Second level cache setup.
 *
 * @author Scott Marlow
 */
public class HibernateSecondLevelCache {

    private static final String DEFAULT_REGION_FACTORY = "org.infinispan.hibernate.cache.v53.InfinispanRegionFactory";

    public static final String CACHE_TYPE = "cachetype";    // shared (jpa) or private (for native applications)
    public static final String CACHE_PRIVATE = "private";
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
        if (Boolean.parseBoolean(mutableProperties.getProperty(ManagedEmbeddedCacheManagerProvider.SHARED, ManagedEmbeddedCacheManagerProvider.DEFAULT_SHARED))) {
            // Set infinispan defaults
            String container = mutableProperties.getProperty(ManagedEmbeddedCacheManagerProvider.CACHE_CONTAINER);
            if (container == null) {
                container = ManagedEmbeddedCacheManagerProvider.DEFAULT_CACHE_CONTAINER;
                mutableProperties.setProperty(ManagedEmbeddedCacheManagerProvider.CACHE_CONTAINER, container);
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
        caches.add(properties.getProperty(IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(COLLECTION_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(NATURAL_ID_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));

        if (properties.containsKey(PENDING_PUTS_CACHE_RESOURCE_PROP)) {
            caches.add(properties.getProperty(PENDING_PUTS_CACHE_RESOURCE_PROP));
        }
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
