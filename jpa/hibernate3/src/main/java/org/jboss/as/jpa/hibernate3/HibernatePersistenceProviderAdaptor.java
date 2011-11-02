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

package org.jboss.as.jpa.hibernate3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.jboss.as.jpa.spi.JtaManager;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate 3.3.x or higher 3.x
 *
 * @author Scott Marlow
 */
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor {

    public static final String SCANNER = "hibernate.ejb.resource_scanner";
    private static final String HIBERNATE_ANNOTATION_SCANNER_CLASS = "org.jboss.as.jpa.hibernate3.HibernateAnnotationScanner";

    @Override
    public void injectJtaManager(JtaManager jtaManager) {
        JBossAppServerJtaPlatform.initJBossAppServerJtaPlatform(jtaManager);
    }

    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        putPropertyIfAbsent(properties, "hibernate.transaction.manager_lookup_class", "org.jboss.as.jpa.hibernate3.JBossAppServerJtaPlatform");
        putPropertyIfAbsent(properties, Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        addAnnotationScanner(pu);
    }

    /**
     * Use reflection to see if we are using Hibernate 3.3.x or older (which doesn't have the
     * org.hibernate.ejb.packaging.Scanner class)
     *
     * @param pu
     */
    private void addAnnotationScanner(PersistenceUnitMetadata pu) {
        try {
            Configuration.class.getClassLoader().loadClass(HIBERNATE_ANNOTATION_SCANNER_CLASS);
            pu.getProperties().put(SCANNER, HIBERNATE_ANNOTATION_SCANNER_CLASS);
        } catch (Throwable ignore) {

        }
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
            result.add(ContextNames.bindInfoFor(toJndiName(cacheManager).toString()).getBinderServiceName());
            return result;
        }
        return null;
    }

    private void putPropertyIfAbsent(Map properties, String property, Object value) {
        if (!properties.containsKey(property)) {
            properties.put(property, value);
        }
    }

    private static JndiName toJndiName(String value) {
        return value.startsWith("java:") ? JndiName.of(value) : JndiName.of("java:jboss").append(value.startsWith("/") ? value.substring(1) : value);
    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        if (pu.getProperties().containsKey(SCANNER)) {
            try {
                Class scanner = Configuration.class.getClassLoader().loadClass(HIBERNATE_ANNOTATION_SCANNER_CLASS);
                // get method for public static void setThreadLocalPersistenceUnitMetadata(final PersistenceUnitMetadata pu) {
                Method setThreadLocalPersistenceUnitMetadata = scanner.getMethod("setThreadLocalPersistenceUnitMetadata", PersistenceUnitMetadata.class);
                setThreadLocalPersistenceUnitMetadata.invoke(null, pu);
            } catch (Throwable ignore) {

            }
        }
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        if (pu.getProperties().containsKey(SCANNER)) {
            // clear backdoor annotation scanner access to pu
            try {
                Class scanner = Configuration.class.getClassLoader().loadClass(HIBERNATE_ANNOTATION_SCANNER_CLASS);
                // get method for public static void clearThreadLocalPersistenceUnitMetadata() {
                Method clearThreadLocalPersistenceUnitMetadata = scanner.getMethod("clearThreadLocalPersistenceUnitMetadata", null);
                clearThreadLocalPersistenceUnitMetadata.invoke(null);
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return null;
    }

}

