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

package org.jboss.as.jpa.hibernate;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadata;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.Map;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor {

    @Override
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        properties.put(Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        properties.put(org.hibernate.ejb.AvailableSettings.SCANNER,"org.jboss.as.jpa.hibernate.HibernateAnnotationScanner");
        properties.put(AvailableSettings.APP_CLASSLOADER, pu.getClassLoader());
        properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform());
    }

    @Override
    public Iterable<ServiceName> getProviderDependencies(PersistenceUnitMetadata pu) {
        String cacheManager;
        // AS7-680 Add BinderService dependency for infinispan hibernate 2LC
        if ((cacheManager = pu.getProperties().getProperty("hibernate.cache.infinispan.cachemanager")) != null) {
            ArrayList<ServiceName> result = new ArrayList<ServiceName>();
            result.add(adjustJndiName(cacheManager));
            return result;
        }
        return null;
    }

    private ServiceName adjustJndiName(String jndiName) {
        jndiName = toJndiName(jndiName).toString();
        int index = jndiName.indexOf("/");
        String namespace = (index > 5) ? jndiName.substring(5, index) : null;
        String binding = (index > 5) ? jndiName.substring(index + 1) : jndiName.substring(5);
        ServiceName naming = (namespace != null) ? ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(namespace) : ContextNames.JAVA_CONTEXT_SERVICE_NAME;
        return naming.append(binding);
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

}

