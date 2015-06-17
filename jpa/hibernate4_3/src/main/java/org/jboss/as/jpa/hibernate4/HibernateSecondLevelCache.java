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

import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate4.infinispan.InfinispanRegionFactory;
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
    public static final String COLLECTION = "collection";
    public static final String ENTITY = "entity";
    public static final String NAME = "name";
    public static final String QUERY = "query";
    public static final String TIMESTAMPS = "timestamps";

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
            cacheSettings.put(COLLECTION, mutableProperties.getProperty(InfinispanRegionFactory.COLLECTION_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE));
            if (Boolean.parseBoolean(mutableProperties.getProperty(AvailableSettings.USE_QUERY_CACHE))) {
                cacheSettings.put(QUERY, mutableProperties.getProperty(InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_QUERY_RESOURCE));
                cacheSettings.put(TIMESTAMPS, mutableProperties.getProperty(InfinispanRegionFactory.TIMESTAMPS_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_QUERY_RESOURCE));
            }
            Notification.addCacheDependencies(Classification.INFINISPAN, cacheSettings);
        }
    }
}
