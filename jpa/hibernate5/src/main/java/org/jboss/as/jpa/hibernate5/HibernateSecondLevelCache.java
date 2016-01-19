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

import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate5.infinispan.InfinispanRegionFactory;
import org.jboss.as.jpa.hibernate5.infinispan.SharedInfinispanRegionFactory;
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
    public static final String COLLECTION = "collection";
    public static final String ENTITY = "entity";
    public static final String IMMUTABLE_ENTITY = "immutable-entity";
    public static final String NAME = "name";
    public static final String NATURAL_ID = "natural-id";
    public static final String QUERY = "query";
    public static final String TIMESTAMPS = "timestamps";
    public static final String PENDING_PUTS = InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME;
    public static final String PENDING_PUTS_CACHE_RESOURCE_PROP ="hibernate.cache.infinispan.pending-puts.cfg";


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
            String container = mutableProperties.getProperty(InfinispanRegionFactory.CACHE_CONTAINER);
            if (container == null) {
                container = InfinispanRegionFactory.DEFAULT_CACHE_CONTAINER;
                mutableProperties.setProperty(InfinispanRegionFactory.CACHE_CONTAINER, container);
            }

            /**
             * AS will need the ServiceBuilder<?> builder that used to be passed to PersistenceProviderAdaptor.addProviderDependencies
             */
            Properties cacheSettings = new Properties();
            cacheSettings.put(CONTAINER, container);
            cacheSettings.put(ENTITY, mutableProperties.getProperty(InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE));
            cacheSettings.put(IMMUTABLE_ENTITY, mutableProperties.getProperty(InfinispanRegionFactory.IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE));
            cacheSettings.put(COLLECTION, mutableProperties.getProperty(InfinispanRegionFactory.COLLECTION_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE));
            cacheSettings.put(NATURAL_ID, mutableProperties.getProperty(InfinispanRegionFactory.NATURAL_ID_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE));
            if (mutableProperties.getProperty(PENDING_PUTS_CACHE_RESOURCE_PROP) != null) {
                cacheSettings.put(PENDING_PUTS, mutableProperties.getProperty(PENDING_PUTS_CACHE_RESOURCE_PROP));
            }
            if (Boolean.parseBoolean(mutableProperties.getProperty(AvailableSettings.USE_QUERY_CACHE))) {
                cacheSettings.put(QUERY, mutableProperties.getProperty(InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_QUERY_RESOURCE));
                cacheSettings.put(TIMESTAMPS, mutableProperties.getProperty(InfinispanRegionFactory.TIMESTAMPS_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_QUERY_RESOURCE));
            }
            Notification.addCacheDependencies(Classification.INFINISPAN, cacheSettings);
        }
    }
}
