/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.persistence.spi.PersistenceProvider;

import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderIntegratorAdaptor;
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

        @Override
        public void addClassFileTransformer(PersistenceUnitMetadata pu) {

        }
    };

    /**
     * Loads the persistence provider adapter
     *
     * @param adapterModule may specify the adapter module name (can be null to use noop provider)
     * @return the persistence provider adaptor for the provider class
     * @throws ModuleLoadException
     */
    public static PersistenceProviderAdaptor loadPersistenceAdapterModule(String adapterModule, final Platform platform, JtaManagerImpl manager) throws
        ModuleLoadException {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        if (adapterModule == null) {
            return noopAdaptor;
        }

        PersistenceProviderAdaptor persistenceProviderAdaptor=null;
        adapterModule = ModuleIdentifierUtil.canonicalModuleIdentifier(adapterModule);
        Module module = moduleLoader.loadModule(adapterModule);
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


    /**
     * Loads the persistence provider integrator adapter
     *
     * @param adapterModule the adapter module name
     * @return the adaptors for the given module
     * @throws ModuleLoadException
     */
    public static List<PersistenceProviderIntegratorAdaptor> loadPersistenceProviderIntegratorModule(
            String adapterModule, Collection<Index> indexes) throws ModuleLoadException {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        List<PersistenceProviderIntegratorAdaptor> persistenceProviderAdaptors = new ArrayList<>();
        adapterModule = ModuleIdentifierUtil.canonicalModuleIdentifier(adapterModule);
        Module module = moduleLoader.loadModule(adapterModule);
        final ServiceLoader<PersistenceProviderIntegratorAdaptor> serviceLoader =
                module.loadService(PersistenceProviderIntegratorAdaptor.class);
        for (PersistenceProviderIntegratorAdaptor adaptor : serviceLoader) {
            persistenceProviderAdaptors.add(adaptor);
            ROOT_LOGGER.debugf("loaded persistence provider integrator adapter %s from %s", adaptor, adapterModule);
            if (adaptor != null) {
                adaptor.injectIndexes(indexes);
            }
        }

        return persistenceProviderAdaptors;
    }

}
