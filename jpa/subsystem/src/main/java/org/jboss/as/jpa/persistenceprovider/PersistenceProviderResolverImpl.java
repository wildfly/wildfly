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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;

import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Implementation of PersistenceProviderResolver
 *
 * @author Scott Marlow
 */
public class PersistenceProviderResolverImpl implements PersistenceProviderResolver {

    private Map<ClassLoader, List<Class<? extends PersistenceProvider>>> persistenceProviderPerClassLoader = new HashMap<>();

    private List<Class<? extends PersistenceProvider>> providers = new CopyOnWriteArrayList<>();

    private static final PersistenceProviderResolverImpl INSTANCE = new PersistenceProviderResolverImpl();

    public static PersistenceProviderResolverImpl getInstance() {
        return INSTANCE;
    }

    public PersistenceProviderResolverImpl() {
    }

    /**
     * Return a new instance of each persistence provider class
     *
     * @return
     */
    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        List<PersistenceProvider> providersCopy = new ArrayList<>(providers.size());

        /**
         * Add the application specified providers first so they are found before the global providers
         */
        synchronized(persistenceProviderPerClassLoader) {
            if (persistenceProviderPerClassLoader.size() > 0) {
                // get the deployment or subdeployment classloader
                ClassLoader deploymentClassLoader = findParentModuleCl(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
                ROOT_LOGGER.tracef("get application level Persistence Provider for classloader %s" , deploymentClassLoader);
                // collect persistence providers associated with deployment/each sub-deployment
                List<Class<? extends PersistenceProvider>> deploymentSpecificPersistenceProviders = persistenceProviderPerClassLoader.get(deploymentClassLoader);
                ROOT_LOGGER.tracef("got application level Persistence Provider list %s" , deploymentSpecificPersistenceProviders);
                if (deploymentSpecificPersistenceProviders != null) {

                    for (Class<? extends PersistenceProvider> providerClass : deploymentSpecificPersistenceProviders) {
                        try {
                            ROOT_LOGGER.tracef("application has its own Persistence Provider %s", providerClass.getName());
                            providersCopy.add(providerClass.newInstance());
                        } catch (InstantiationException e) {
                            throw JpaLogger.ROOT_LOGGER.couldNotCreateInstanceProvider(e, providerClass.getName());
                        } catch (IllegalAccessException e) {
                            throw JpaLogger.ROOT_LOGGER.couldNotCreateInstanceProvider(e, providerClass.getName());
                        }
                    }
                }
            }
        }

        // add global persistence providers last (so application packaged providers have priority)
        for (Class<?> providerClass : providers) {
            try {
                providersCopy.add((PersistenceProvider) providerClass.newInstance());
                ROOT_LOGGER.tracef("returning global (module) Persistence Provider %s", providerClass.getName());
            } catch (InstantiationException e) {
                throw JpaLogger.ROOT_LOGGER.couldNotCreateInstanceProvider(e, providerClass.getName());
            } catch (IllegalAccessException e) {
                throw JpaLogger.ROOT_LOGGER.couldNotCreateInstanceProvider(e, providerClass.getName());
            }
        }
        return providersCopy;
    }

    @Override
    public void clearCachedProviders() {
        providers.clear();
    }

    /**
     * Cleared at application undeployment time to remove any persistence providers that were deployed with the application
     *
     * @param deploymentClassLoaders
     */
    public void clearCachedDeploymentSpecificProviders(Set<ClassLoader> deploymentClassLoaders) {

        synchronized(persistenceProviderPerClassLoader) {
            for (ClassLoader deploymentClassLoader: deploymentClassLoaders) {
                persistenceProviderPerClassLoader.remove(deploymentClassLoader);
            }
        }
    }

    /**
     * Set at application deployment time to the persistence providers packaged in the application
     *
     * @param persistenceProvider
     * @param deploymentClassLoaders
     */
    public void addDeploymentSpecificPersistenceProvider(PersistenceProvider persistenceProvider, Set<ClassLoader> deploymentClassLoaders) {

        synchronized(persistenceProviderPerClassLoader) {

            for (ClassLoader deploymentClassLoader: deploymentClassLoaders) {
                List<Class<? extends PersistenceProvider>> list = persistenceProviderPerClassLoader.get(deploymentClassLoader);
                ROOT_LOGGER.tracef("getting persistence provider list (%s) for deployment (%s)", list, deploymentClassLoader );
                if (list == null) {
                    list = new ArrayList<>();
                    persistenceProviderPerClassLoader.put(deploymentClassLoader, list);
                    ROOT_LOGGER.tracef("saving new persistence provider list (%s) for deployment (%s)", list, deploymentClassLoader );
                }
                list.add(persistenceProvider.getClass());
                ROOT_LOGGER.tracef("added new persistence provider (%s) to provider list (%s)", persistenceProvider.getClass().getName(), list);
            }
        }
    }

    /**
     * If a custom CL is in use we want to get the module CL it delegates to
     * @param classLoader The current CL
     * @returnThe corresponding module CL
     */
    private ClassLoader findParentModuleCl(ClassLoader classLoader) {
        ClassLoader c = classLoader;
        while (c != null && !(c instanceof ModuleClassLoader)) {
            c = c.getParent();
        }
        return c;
    }


    public void addPersistenceProvider(PersistenceProvider persistenceProvider) {
        providers.add(persistenceProvider.getClass());
    }

}
