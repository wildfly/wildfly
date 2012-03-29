/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerConfigurationService;
import org.jboss.as.jpa.hibernate4.infinispan.InfinispanRegionFactory;
import org.jboss.as.jpa.hibernate4.infinispan.SharedInfinispanRegionFactory;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

import java.util.Properties;

/**
 * Second level cache setup.
 *
 * @author Scott Marlow
 */
public class HibernateSecondLevelCache {

    private static final String DEFAULT_REGION_FACTORY = SharedInfinispanRegionFactory.class.getName();

    public static void addSecondLevelCacheDependencies(ServiceRegistry registry, ServiceTarget target, ServiceBuilder<?> builder, PersistenceUnitMetadata pu) {
        Properties properties = pu.getProperties();

        if (properties.getProperty(AvailableSettings.CACHE_REGION_PREFIX) == null) {
            // cache entries for this PU will be identified by scoped pu name + Entity class name
            properties.put(AvailableSettings.CACHE_REGION_PREFIX, pu.getScopedPersistenceUnitName());
        }
        String regionFactory = properties.getProperty(AvailableSettings.CACHE_REGION_FACTORY);
        if (regionFactory == null) {
            regionFactory = DEFAULT_REGION_FACTORY;
            properties.setProperty(AvailableSettings.CACHE_REGION_FACTORY, regionFactory);
        }
        if (regionFactory.equals(DEFAULT_REGION_FACTORY)) {
            // Set infinispan defaults
            String container = properties.getProperty(InfinispanRegionFactory.CACHE_CONTAINER);
            if (container == null) {
                container = InfinispanRegionFactory.DEFAULT_CACHE_CONTAINER;
                properties.setProperty(InfinispanRegionFactory.CACHE_CONTAINER, container);
            }
            ServiceName classLoaderServiceName = EmbeddedCacheManagerConfigurationService.getClassLoaderServiceName(container);
            synchronized (HibernateSecondLevelCache.class) {
                if (registry.getService(classLoaderServiceName) == null) {
                    target.addService(classLoaderServiceName, new ValueService<ClassLoader>(new ImmediateValue<ClassLoader>(SharedInfinispanRegionFactory.class.getClassLoader()))).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
                }
            }
            String entity = properties.getProperty(InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE);
            String collection = properties.getProperty(InfinispanRegionFactory.COLLECTION_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_ENTITY_RESOURCE);
            String query = properties.getProperty(InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_QUERY_RESOURCE);
            String timestamps = properties.getProperty(InfinispanRegionFactory.TIMESTAMPS_CACHE_RESOURCE_PROP, InfinispanRegionFactory.DEF_QUERY_RESOURCE);
            builder.addDependency(CacheConfigurationService.getServiceName(container, entity));
            builder.addDependency(CacheConfigurationService.getServiceName(container, collection));
            builder.addDependency(CacheConfigurationService.getServiceName(container, timestamps));
            builder.addDependency(CacheConfigurationService.getServiceName(container, query));
        }
    }
}
