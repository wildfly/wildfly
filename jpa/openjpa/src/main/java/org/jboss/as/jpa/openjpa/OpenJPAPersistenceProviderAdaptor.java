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

package org.jboss.as.jpa.openjpa;

import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;

/**
 * Implements the {@link PersistenceProviderAdaptor} for OpenJPA 2.x.
 *
 * @author Antti Laisi
 */
public class OpenJPAPersistenceProviderAdaptor implements PersistenceProviderAdaptor {

    private static final String TRANSACTION_MODE = "openjpa.TransactionMode";
    private static final String MANAGED_RUNTIME  = "openjpa.ManagedRuntime";
    private static final String METADATA_FACTORY = "openjpa.MetaDataFactory";
    private static final String MANAGED = "managed";
    private static final String JNDI_TRANSACTION_MANAGER_NAME_JAVA_JBOSS_TRANSACTION_MANAGER = "jndi(TransactionManagerName=java:jboss/TransactionManager)";

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        if(!pu.getProperties().containsKey(TRANSACTION_MODE)) {
            properties.put(TRANSACTION_MODE, MANAGED);
        }
        if(!pu.getProperties().containsKey(MANAGED_RUNTIME)) {
            properties.put(MANAGED_RUNTIME, JNDI_TRANSACTION_MANAGER_NAME_JAVA_JBOSS_TRANSACTION_MANAGER);
        }
        if(!pu.getProperties().containsKey(METADATA_FACTORY)) {
            properties.put(METADATA_FACTORY, JBossPersistenceMetaDataFactory.class.getName());
        }
    }

    @Override
    public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        JBossPersistenceMetaDataFactory.setThreadLocalPersistenceUnitMetadata(pu);
    }

    @Override
    public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        JBossPersistenceMetaDataFactory.clearThreadLocalPersistenceUnitMetadata();
    }

    @Override
    public void injectJtaManager(JtaManager jtaManager) {

    }

    @Override
    public void injectPlatform(Platform platform) {

    }


    @Override
    public void addProviderDependencies(PersistenceUnitMetadata pu) {
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
        JBossPersistenceMetaDataFactory.cleanup(pu);
    }

    @Override
    public Object beanManagerLifeCycle(BeanManager beanManager) {
        return null;
    }

    @Override
    public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {

    }

}
