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

package org.jboss.as.jpa.openjpa;

import java.util.Map;

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

}
