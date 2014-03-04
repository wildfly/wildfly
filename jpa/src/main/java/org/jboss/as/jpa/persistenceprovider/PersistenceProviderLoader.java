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

package org.jboss.as.jpa.persistenceprovider;

import java.util.ServiceLoader;

import javax.persistence.spi.PersistenceProvider;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
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
     * Loads the specified JPA persistence provider module name
     *
     * @param moduleName
     * @throws ModuleLoadException
     */
    public static void loadProviderModuleByName(String moduleName) throws ModuleLoadException {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
        final ServiceLoader<PersistenceProvider> serviceLoader =
            module.loadService(PersistenceProvider.class);
        if (serviceLoader != null) {
            for (PersistenceProvider provider1 : serviceLoader) {
                // persistence provider jar may contain multiple provider service implementations
                // save each provider
                PersistenceProviderResolverImpl.getInstance().addPersistenceProvider(provider1);
            }
        }
    }

}

