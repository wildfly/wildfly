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
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jboss.as.jpa.spi.JtaManager;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

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
        putPropertyIfAbsent(pu, properties, Environment.TRANSACTION_MANAGER_STRATEGY, JBossAppServerJtaPlatform.class.getName());
        putPropertyIfAbsent(pu, properties, Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
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
    public void addProviderDependencies(ServiceRegistry registry, ServiceTarget target, ServiceBuilder<?> builder, PersistenceUnitMetadata pu) {
        if (Boolean.parseBoolean(pu.getProperties().getProperty(Environment.USE_SECOND_LEVEL_CACHE))) {
            HibernateSecondLevelCache.addSecondLevelCacheDependencies(registry, target, builder, pu);
        }
    }

    private void putPropertyIfAbsent(PersistenceUnitMetadata pu, Map properties, String property, Object value) {
        if (!pu.getProperties().containsKey(property)) {
            properties.put(property, value);
        }
    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        if (pu.getProperties().containsKey(SCANNER)) {
            try {
                Class<?> scanner = Configuration.class.getClassLoader().loadClass(HIBERNATE_ANNOTATION_SCANNER_CLASS);
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
                Class<?> scanner = Configuration.class.getClassLoader().loadClass(HIBERNATE_ANNOTATION_SCANNER_CLASS);
                // get method for public static void clearThreadLocalPersistenceUnitMetadata() {
                Method clearThreadLocalPersistenceUnitMetadata = scanner.getMethod("clearThreadLocalPersistenceUnitMetadata");
                clearThreadLocalPersistenceUnitMetadata.invoke(null);
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return null;
    }

    @Override
    public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
        return true;
    }

    @Override
    public void cleanup(PersistenceUnitMetadata pu) {
        HibernateAnnotationScanner.cleanup(pu);
    }
}

