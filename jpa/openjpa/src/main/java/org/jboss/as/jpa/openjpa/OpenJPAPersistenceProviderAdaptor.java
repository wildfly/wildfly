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

package org.jboss.as.jpa.openjpa;

import java.util.Map;

import org.jboss.as.jpa.spi.JtaManager;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Implements the {@link PersistenceProviderAdaptor} for OpenJPA 2.x.
 *
 * @author Antti Laisi
 */
public class OpenJPAPersistenceProviderAdaptor implements PersistenceProviderAdaptor {

    private static final String TRANSACTION_MODE = "openjpa.TransactionMode";
    private static final String MANAGED_RUNTIME  = "openjpa.ManagedRuntime";
    private static final String METADATA_FACTORY = "openjpa.MetaDataFactory";

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        if(!pu.getProperties().containsKey(TRANSACTION_MODE)) {
            properties.put(TRANSACTION_MODE, "managed");
        }
        if(!pu.getProperties().containsKey(MANAGED_RUNTIME)) {
            properties.put(MANAGED_RUNTIME, "jndi(TransactionManagerName=java:jboss/TransactionManager)");
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
    public void addProviderDependencies(ServiceRegistry registry, ServiceTarget target, ServiceBuilder<?> builder, PersistenceUnitMetadata pu) {
    }

    @Override
    public ManagementAdaptor getManagementAdaptor() {
        return null;
    }

    @Override
    public void cleanup(PersistenceUnitMetadata pu) {
        JBossPersistenceMetaDataFactory.cleanup(pu);
    }

}
