/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.jboss.as.jpa.hibernate4.management.HibernateManagementAdaptor;
import org.jboss.as.jpa.spi.JtaManager;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.msc.service.ServiceName;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor {

    private static final String DEFAULT_REGION_FACTORY = "org.jboss.as.jpa.hibernate.cache.infinispan.InfinispanRegionFactory";
    private static final String DEFAULT_CACHE_CONTAINER = "hibernate";
    private static final String DEFAULT_ENTITY_CACHE = "entity";
    private static final String DEFAULT_COLLECTION_CACHE = "entity";
    private static final String DEFAULT_QUERY_CACHE = "local-query";
    private static final String DEFAULT_TIMESTAMPS_CACHE = "timestamps";
    private volatile JBossAppServerJtaPlatform appServerJtaPlatform;

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
        appServerJtaPlatform = new JBossAppServerJtaPlatform(jtaManager);
    }

    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        putPropertyIfAbsent(pu, properties, Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        putPropertyIfAbsent(pu, properties, org.hibernate.ejb.AvailableSettings.SCANNER, "org.jboss.as.jpa.hibernate4.HibernateAnnotationScanner");
        properties.put(AvailableSettings.APP_CLASSLOADER, pu.getClassLoader());
        putPropertyIfAbsent(pu, properties, AvailableSettings.JTA_PLATFORM, appServerJtaPlatform);
        properties.remove(AvailableSettings.TRANSACTION_MANAGER_STRATEGY);  // remove legacy way of specifying TX manager (conflicts with JTA_PLATFORM)
        // TODO:  use org.hibernate.ejb.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME for the following after its added to Hibernate 4.0.1.Final
        putPropertyIfAbsent(pu,properties, org.hibernate.ejb.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME, pu.getScopedPersistenceUnitName());
        putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME, pu.getScopedPersistenceUnitName());
        if (!pu.getProperties().containsKey(AvailableSettings.SESSION_FACTORY_NAME)) {
            putPropertyIfAbsent(pu, properties, AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, Boolean.FALSE);
        }
    }

    @Override
    public Iterable<ServiceName> getProviderDependencies(PersistenceUnitMetadata pu) {
        Properties properties = pu.getProperties();
        if (Boolean.parseBoolean(properties.getProperty("hibernate.cache.use_second_level_cache"))) {
            if (properties.getProperty("hibernate.cache.region_prefix") == null) {
                // cache entries for this PU will be identified by scoped pu name + Entity class name
                properties.put("hibernate.cache.region_prefix", pu.getScopedPersistenceUnitName());
            }
            String regionFactory = properties.getProperty("hibernate.cache.region.factory_class");
            if (regionFactory == null) {
                regionFactory = DEFAULT_REGION_FACTORY;
                properties.setProperty("hibernate.cache.region.factory_class", regionFactory);
            }
            if (regionFactory.equals(DEFAULT_REGION_FACTORY)) {
                // Set infinispan defaults
                String container = properties.getProperty("hibernate.cache.infinispan.container");
                if (container == null) {
                    container = DEFAULT_CACHE_CONTAINER;
                    properties.setProperty("hibernate.cache.infinispan.container", container);
                }
                String entity = properties.getProperty("hibernate.cache.infinispan.entity.cfg", DEFAULT_ENTITY_CACHE);
                String collection = properties.getProperty("hibernate.cache.infinispan.collection.cfg", DEFAULT_COLLECTION_CACHE);
                String query = properties.getProperty("hibernate.cache.infinispan.query.cfg", DEFAULT_QUERY_CACHE);
                String timestamps = properties.getProperty("hibernate.cache.infinispan.timestamps.cfg", DEFAULT_TIMESTAMPS_CACHE);
                Set<ServiceName> result = new HashSet<ServiceName>();
                result.add(this.getCacheConfigServiceName(container, entity));
                result.add(this.getCacheConfigServiceName(container, collection));
                result.add(this.getCacheConfigServiceName(container, timestamps));
                result.add(this.getCacheConfigServiceName(container, query));
                return result;
            }
        }
        return null;
    }

    private ServiceName getCacheConfigServiceName(String container, String cache) {
        return ServiceName.JBOSS.append("infinispan", container, cache, "config");
    }

    private void putPropertyIfAbsent(PersistenceUnitMetadata pu,Map properties, String property, Object value) {
        if (!pu.getProperties().containsKey(property)) {
            properties.put(property, value);
        }
    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        // set backdoor annotation scanner access to pu
        HibernateAnnotationScanner.setThreadLocalPersistenceUnitMetadata(pu);
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        // clear backdoor annotation scanner access to pu
        HibernateAnnotationScanner.clearThreadLocalPersistenceUnitMetadata();
    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return HibernateManagementAdaptor.getInstance();
    }

    public void cleanup(PersistenceUnitMetadata pu) {
        HibernateAnnotationScanner.cleanup(pu);
    }
}

