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

package org.jboss.as.jpa.processor;

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.util.Map;
import java.util.ServiceLoader;

import org.jboss.as.jpa.spi.JtaManager;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * Loads persistence provider adaptors
 *
 * @author Scott Marlow
 */
public class PersistenceProviderAdaptorLoader {

    private static final PersistenceProviderAdaptor noopAdaptor = new PersistenceProviderAdaptor() {

        @Override
        public void injectJtaManager(JtaManager jtaManager) {
        }

        @Override
        public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        }

        @Override
        public Iterable<ServiceName> getProviderDependencies(PersistenceUnitMetadata pu) {
            return null;
        }

        @Override
        public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        }

        @Override
        public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
        }

        @Override
        public ManagementAdaptor getManagementAdaptor() {
            return null;
        }
    };

    /**
     * Loads the persistence provider adapter
     *
     * @param adapterModule may specify the adapter module name (can be null to use noop provider)
     * @return the persistence provider adaptor for the provider class
     * @throws ModuleLoadException
     */
    public static PersistenceProviderAdaptor loadPersistenceAdapterModule(String adapterModule) throws
        ModuleLoadException {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        if (adapterModule == null) {
            return noopAdaptor;
        }

        PersistenceProviderAdaptor persistenceProviderAdaptor=null;

        Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(adapterModule));
        final ServiceLoader<PersistenceProviderAdaptor> serviceLoader =
            module.loadService(PersistenceProviderAdaptor.class);
        if (serviceLoader != null) {
            for (PersistenceProviderAdaptor adaptor : serviceLoader) {
                if (persistenceProviderAdaptor != null) {
                    throw MESSAGES.multipleAdapters(adapterModule);
                }
                persistenceProviderAdaptor = adaptor;
                JPA_LOGGER.debugf("loaded persistence provider adapter %s", adapterModule);
            }
            if (persistenceProviderAdaptor != null) {
                persistenceProviderAdaptor.injectJtaManager(JtaManagerImpl.getInstance());
            }
        }

        return persistenceProviderAdaptor;
    }
}
