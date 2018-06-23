/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.spi.PersistenceProvider;

import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.Platform;

/**
 * Deploy JPA Persistence providers that are found in the application deployment.
 *
 * @author Scott Marlow
 */
public class PersistenceProviderHandler {

    private static final String PERSISTENCE_PROVIDER_CLASSNAME = PersistenceProvider.class.getName();

    public static void deploy(final DeploymentPhaseContext phaseContext, final Platform platform) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        final ServicesAttachment servicesAttachment = deploymentUnit.getAttachment(Attachments.SERVICES);
        if (module != null && servicesAttachment != null) {
            final ModuleClassLoader deploymentModuleClassLoader = module.getClassLoader();
            PersistenceProvider provider;

            // collect list of persistence providers packaged with the application
            final List<String> providerNames = servicesAttachment.getServiceImplementations(PERSISTENCE_PROVIDER_CLASSNAME);
            List<PersistenceProvider> providerList = new ArrayList<PersistenceProvider>();

            for (String providerName : providerNames) {
                try {
                    final Class<? extends PersistenceProvider> providerClass = deploymentModuleClassLoader.loadClass(providerName).asSubclass(PersistenceProvider.class);
                    final Constructor<? extends PersistenceProvider> constructor = providerClass.getConstructor();
                    provider = constructor.newInstance();
                    providerList.add(provider);
                    JpaLogger.ROOT_LOGGER.tracef("deployment %s is using its own copy of %s", deploymentUnit.getName(), providerName);

                } catch (Exception e) {
                    throw JpaLogger.ROOT_LOGGER.cannotDeployApp(e, providerName);
                }
            }

            if (providerList.size() > 0) {
                final String adapterClass = deploymentUnit.getAttachment(JpaAttachments.ADAPTOR_CLASS_NAME);
                PersistenceProviderAdaptor adaptor;
                if (adapterClass != null) {
                    try {
                        adaptor = (PersistenceProviderAdaptor) deploymentModuleClassLoader.loadClass(adapterClass).newInstance();
                        adaptor.injectJtaManager(JtaManagerImpl.getInstance());
                        adaptor.injectPlatform(platform);
                        ArrayList<PersistenceProviderAdaptor> adaptorList = new ArrayList<>();
                        adaptorList.add(adaptor);
                        PersistenceProviderDeploymentHolder.savePersistenceProviderInDeploymentUnit(deploymentUnit, providerList, adaptorList);
                    } catch (InstantiationException e) {
                        throw JpaLogger.ROOT_LOGGER.cannotCreateAdapter(e, adapterClass);
                    } catch (IllegalAccessException e) {
                        throw JpaLogger.ROOT_LOGGER.cannotCreateAdapter(e, adapterClass);
                    } catch (ClassNotFoundException e) {
                        throw JpaLogger.ROOT_LOGGER.cannotCreateAdapter(e, adapterClass);
                    }
                } else {
                    // register the provider (no adapter specified)
                    PersistenceProviderDeploymentHolder.savePersistenceProviderInDeploymentUnit(deploymentUnit, providerList, null);
                }
            }
        }
    }

    public static void finishDeploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        PersistenceProviderDeploymentHolder persistenceProviderDeploymentHolder  = PersistenceProviderDeploymentHolder.getPersistenceProviderDeploymentHolder(deploymentUnit);
        Map<String, PersistenceProvider> providerMap = persistenceProviderDeploymentHolder != null ?
                persistenceProviderDeploymentHolder.getProviders() : null;

        if (providerMap != null) {
            Set<ClassLoader> deploymentClassLoaders = allDeploymentModuleClassLoaders(deploymentUnit);
            synchronized (providerMap){
                for(Map.Entry<String, PersistenceProvider> kv: providerMap.entrySet()){
                    PersistenceProviderResolverImpl.getInstance().addDeploymentSpecificPersistenceProvider(kv.getValue(), deploymentClassLoaders);
                }
            }
        }
    }

    public static void undeploy(final DeploymentUnit deploymentUnit) {
        Set<ClassLoader> deploymentClassLoaders = allDeploymentModuleClassLoaders(deploymentUnit);
        PersistenceProviderResolverImpl.getInstance().clearCachedDeploymentSpecificProviders(deploymentClassLoaders);
    }

    /**
     * returns the toplevel deployment module classloader and all subdeployment classloaders
     *
     * @param deploymentUnit
     * @return
     */
    private static Set<ClassLoader> allDeploymentModuleClassLoaders(DeploymentUnit deploymentUnit) {
        Set<ClassLoader> deploymentClassLoaders = new HashSet<ClassLoader>();
        final DeploymentUnit topDeploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        final Module toplevelModule = topDeploymentUnit.getAttachment(Attachments.MODULE);
        if (toplevelModule != null) {
            deploymentClassLoaders.add(toplevelModule.getClassLoader());
            final List<DeploymentUnit> subDeployments = topDeploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            for (DeploymentUnit subDeploymentUnit: subDeployments) {
                final Module subDeploymentModule = subDeploymentUnit.getAttachment(Attachments.MODULE);
                if (subDeploymentModule != null) {
                    deploymentClassLoaders.add(subDeploymentModule.getClassLoader());
                }
            }
        }
        return deploymentClassLoaders;
    }
}

