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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.CacheCommandFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cfg.AvailableSettings;
import org.infinispan.commands.module.ModuleCommandFactory;
import org.infinispan.factories.GlobalComponentRegistry;
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

    private final Collection<BaseRegion> regions = new LinkedList<>();

    public InfinispanRegionFactory() {
        super();
    }

    public InfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return this.addRegion(super.buildCollectionRegion(regionName, properties, metadata));
    }

    @Override
    public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return this.addRegion(super.buildEntityRegion(regionName, properties, metadata));
    }

    @Override
    public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return this.addRegion(super.buildNaturalIdRegion(regionName, properties, metadata));
    }

    @Override
    public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
        return this.addRegion(super.buildQueryResultsRegion(regionName, properties));
    }

    @Override
    public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
        return this.addRegion(super.buildTimestampsRegion(regionName, properties));
    }

    private <R extends Region> R addRegion(R region) {
        this.regions.add((BaseRegion) region);
        return region;
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
        // Find a suitable service name to represent this session factory instance
        String name = properties.getProperty(AvailableSettings.SESSION_FACTORY_NAME);
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        HibernateSecondLevelCache.addSecondLevelCacheDependencies(properties, null);

        Properties cacheSettings = new Properties();
        cacheSettings.put(HibernateSecondLevelCache.CACHE_TYPE,CACHE_PRIVATE);
        cacheSettings.put(HibernateSecondLevelCache.CONTAINER, container);
        if (name != null) {
            cacheSettings.put(HibernateSecondLevelCache.NAME, name);
        }


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

    @Override
    protected void stopCacheRegions() {
        // Workaround HHH-10545 - Don't invoke super implementation - it will start the default cache!
        this.getCacheCommandFactory().clearRegions(this.regions.stream().map(region -> region.getName()).collect(Collectors.toList()));
        // Workaround HHH-10546
        this.regions.forEach(region -> {
            region.getCache().stop();
            this.getCacheManager().undefineConfiguration(region.getCache().getName());
        });
        this.regions.clear();
    }

    private CacheCommandFactory getCacheCommandFactory() {
        GlobalComponentRegistry components = this.getCacheManager().getGlobalComponentRegistry();
        @SuppressWarnings("unchecked")
        Map<Byte, ModuleCommandFactory> factories = (Map<Byte, ModuleCommandFactory>) components.getComponent("org.infinispan.modules.command.factories");
        return factories.values().stream().filter(factory -> factory instanceof CacheCommandFactory).map(factory -> (CacheCommandFactory) factory).findFirst().orElseThrow(() -> new IllegalStateException());
    }
}
