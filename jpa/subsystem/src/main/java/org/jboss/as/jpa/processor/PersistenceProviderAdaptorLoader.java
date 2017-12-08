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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.Map;
import java.util.ServiceLoader;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.spi.PersistenceProvider;

import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;

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
        public void injectPlatform(Platform platform) {

        }

        @Override
        public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
        }

        @Override
        public void addProviderDependencies(PersistenceUnitMetadata pu) {
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

        @Override
        public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
            return true;
        }

        @Override
        public void cleanup(PersistenceUnitMetadata pu) {
        }

        @Override
        public Object beanManagerLifeCycle(BeanManager beanManager) {
            return null;
        }

        @Override
        public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {

        }
    };

    /**
     * Loads the persistence provider adapter
     *
     * @param adapterModule may specify the adapter module name (can be null to use noop provider)
     * @return the persistence provider adaptor for the provider class
     * @throws ModuleLoadException
     */
    public static PersistenceProviderAdaptor loadPersistenceAdapterModule(final String adapterModule, final Platform platform, JtaManagerImpl manager) throws
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
                    throw JpaLogger.ROOT_LOGGER.multipleAdapters(adapterModule);
                }
                persistenceProviderAdaptor = adaptor;
                ROOT_LOGGER.debugf("loaded persistence provider adapter %s", adapterModule);
            }
            if (persistenceProviderAdaptor != null) {
                persistenceProviderAdaptor.injectJtaManager(manager);
                persistenceProviderAdaptor.injectPlatform(platform);
            }
        }

        return persistenceProviderAdaptor;
    }

    /**
     * Loads the persistence provider adapter
     *
     * @param persistenceProvider classloader will be used to load the persistence provider adapter
     * @return the persistence provider adaptor for the provider class
     */
    public static PersistenceProviderAdaptor loadPersistenceAdapter(final PersistenceProvider persistenceProvider, final Platform platform, final JtaManagerImpl jtaManager) {
        PersistenceProviderAdaptor persistenceProviderAdaptor=null;

        final ServiceLoader<PersistenceProviderAdaptor> serviceLoader =
                ServiceLoader.load(PersistenceProviderAdaptor.class, persistenceProvider.getClass().getClassLoader());

        if (serviceLoader != null) {
            for (PersistenceProviderAdaptor adaptor : serviceLoader) {
                if (persistenceProviderAdaptor != null) {
                    throw JpaLogger.ROOT_LOGGER.classloaderHasMultipleAdapters(persistenceProvider.getClass().getClassLoader().toString());
                }
                persistenceProviderAdaptor = adaptor;
                ROOT_LOGGER.debugf("loaded persistence provider adapter %s from classloader %s",
                        persistenceProviderAdaptor.getClass().getName(),
                        persistenceProvider.getClass().getClassLoader().toString());
            }
            if (persistenceProviderAdaptor != null) {
                persistenceProviderAdaptor.injectJtaManager(jtaManager);
                persistenceProviderAdaptor.injectPlatform(platform);
            }
        }

        return persistenceProviderAdaptor == null ?
                noopAdaptor:
                persistenceProviderAdaptor;
    }

}
