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

import java.util.ArrayList;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.jboss.as.jpa.hibernate4.management.HibernateManagementAdaptor;
import org.jboss.as.jpa.spi.JtaManager;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor {


    private volatile JBossAppServerJtaPlatform appServerJtaPlatform;

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
        appServerJtaPlatform = new JBossAppServerJtaPlatform(jtaManager);
    }

    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        putPropertyIfAbsent(properties, Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        putPropertyIfAbsent(properties, org.hibernate.ejb.AvailableSettings.SCANNER, "org.jboss.as.jpa.hibernate4.HibernateAnnotationScanner");
        properties.put(AvailableSettings.APP_CLASSLOADER, pu.getClassLoader());
        putPropertyIfAbsent(properties, AvailableSettings.JTA_PLATFORM, appServerJtaPlatform);
        properties.remove(AvailableSettings.TRANSACTION_MANAGER_STRATEGY);  // remove legacy way of specifying TX manager (conflicts with JTA_PLATFORM)
    }

    @Override
    public Iterable<ServiceName> getProviderDependencies(PersistenceUnitMetadata pu) {
        //
        String cacheManager = pu.getProperties().getProperty("hibernate.cache.infinispan.cachemanager");
        String useCache = pu.getProperties().getProperty("hibernate.cache.use_second_level_cache");
        String regionFactoryClass = pu.getProperties().getProperty("hibernate.cache.region.factory_class");
        if ((useCache != null && useCache.equalsIgnoreCase("true")) ||
            cacheManager != null) {
            if (regionFactoryClass == null) {
                regionFactoryClass = "org.hibernate.cache.infinispan.JndiInfinispanRegionFactory";
                pu.getProperties().put("hibernate.cache.region.factory_class", regionFactoryClass);
            }
            if (cacheManager == null) {
                cacheManager = "java:jboss/infinispan/hibernate";
                pu.getProperties().put("hibernate.cache.infinispan.cachemanager", cacheManager);
            }
            if (pu.getProperties().getProperty("hibernate.cache.region_prefix") == null) {
                // cache entries for this PU will be identified by scoped pu name + Entity class name
                pu.getProperties().put("hibernate.cache.region_prefix", pu.getScopedPersistenceUnitName());
            }
            ArrayList<ServiceName> result = new ArrayList<ServiceName>();
            result.add(adjustJndiName(cacheManager));
            return result;
        }
        return null;
    }

    private void putPropertyIfAbsent(Map properties, String property, Object value) {
        if (!properties.containsKey(property)) {
            properties.put(property, value);
        }
    }

    private ServiceName adjustJndiName(String jndiName) {
        jndiName = toJndiName(jndiName).toString();
        return ContextNames.bindInfoFor(jndiName).getBinderServiceName();
    }

    private static JndiName toJndiName(String value) {
        return value.startsWith("java:") ? JndiName.of(value) : JndiName.of("java:jboss").append(value.startsWith("/") ? value.substring(1) : value);
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

}

