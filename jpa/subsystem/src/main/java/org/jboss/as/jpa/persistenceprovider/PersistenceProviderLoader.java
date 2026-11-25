/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.persistenceprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import jakarta.persistence.spi.PersistenceProvider;

import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * For loading persistence provider modules
 *
 * @author Scott Marlow
 */
public class PersistenceProviderLoader {

    /**
     * pre-loads the default persistence provider
     *
     * @throws ModuleLoadException
     */
    public static void loadDefaultProvider() throws ModuleLoadException {
        String defaultProviderModule = Configuration.getDefaultProviderModuleName();
        loadProviderModuleByName(defaultProviderModule);
    }

    /**
     * Loads the specified Jakarta Persistence persistence provider module
     *
     * @param moduleName is the static module to be loaded
     * @throws ModuleLoadException
     * @return list of persistence providers in specified module
     *
     * Note: side effect of saving loaded persistence providers to static api in jakarta.persistence.spi.PersistenceProvider.
     */
    public static List<PersistenceProvider> loadProviderModuleByName(String moduleName) throws ModuleLoadException {
        moduleName = ModuleIdentifierUtil.parseCanonicalModuleIdentifier(moduleName);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        Module module = moduleLoader.loadModule(moduleName);
        final ServiceLoader<PersistenceProvider> serviceLoader =
            module.loadService(PersistenceProvider.class);
        List<PersistenceProvider> result = new ArrayList<>();
        if (serviceLoader != null) {
            for (PersistenceProvider provider1 : serviceLoader) {
                // persistence provider jar may contain multiple provider service implementations
                // save each provider
                PersistenceProviderResolverImpl.getInstance().addPersistenceProvider(provider1);
                result.add(provider1);
            }
        }
        return result;
    }

    public static PersistenceProvider loadProviderFromDeployment(ClassLoader classLoader, String persistenceProviderClassName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (PersistenceProvider) classLoader.loadClass(persistenceProviderClassName).newInstance();
    }
}

