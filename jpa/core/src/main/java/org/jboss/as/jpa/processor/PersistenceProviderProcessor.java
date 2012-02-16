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

import static org.jboss.as.jpa.JpaLogger.ROOT_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.spi.PersistenceProvider;

import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

/**
 * Deploy JPA Persistence providers that are found in the application deployment.
 *
 * @author Scott Marlow
 */
public class PersistenceProviderProcessor implements DeploymentUnitProcessor {

    private static final String PERSISTENCE_PROVIDER_CLASSNAME = PersistenceProvider.class.getName();
    /**
     * {@inheritDoc}
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
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
                    Set<ClassLoader> deploymentClassLoaders = allDeploymentModuleClassLoaders(deploymentUnit);
                    ROOT_LOGGER.debugf("Deployment has its own persistence provider %s associated with classloaders %s", providerClass, deploymentClassLoaders.toString());

                    // register persistence provider so javax.persistence.Persistence.createEntityManagerFactory can find it
                    PersistenceProviderResolverImpl.getInstance().addDeploymentSpecificPersistenceProvider(provider, deploymentClassLoaders);

                    // TODO: instead of passing provider in via PersistenceProviderDeploymentHolder,
                    // it should instead be looked up via PersistenceProviderResolverImpl (still need to pass adapter in
                    // or add mechanism to deal with it on a per deployment basis).
                    providerList.add(provider);

                } catch (Exception e) {
                    throw MESSAGES.cannotDeployApp(e, providerName);
                }
            }


            if (providerList.size() > 0) {
                final String adapterClass = deploymentUnit.getAttachment(JpaAttachments.ADAPTOR_CLASS_NAME);
                PersistenceProviderAdaptor adaptor;
                if (adapterClass != null) {
                    try {
                        adaptor = (PersistenceProviderAdaptor) deploymentModuleClassLoader.loadClass(adapterClass).newInstance();
                        adaptor.injectJtaManager(JtaManagerImpl.getInstance());
                        deploymentUnit.putAttachment(JpaAttachments.DEPLOYED_PERSISTENCE_PROVIDER, new PersistenceProviderDeploymentHolder(providerList, adaptor));
                    } catch (InstantiationException e) {
                        throw MESSAGES.cannotCreateAdapter(e, adapterClass);
                    } catch (IllegalAccessException e) {
                        throw MESSAGES.cannotCreateAdapter(e, adapterClass);
                    } catch (ClassNotFoundException e) {
                        throw MESSAGES.cannotCreateAdapter(e, adapterClass);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(final DeploymentUnit deploymentUnit) {
        DeploymentUnit toplevelDeploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        final Module module = toplevelDeploymentUnit.getAttachment(Attachments.MODULE);
        Set<ClassLoader> deploymentClassLoaders = allDeploymentModuleClassLoaders(deploymentUnit);

        PersistenceProviderResolverImpl.getInstance().clearCachedDeploymentSpecificProviders(deploymentClassLoaders);
    }

    /**
     * returns the toplevel deployment module classloader and all subdeployment classloaders
     *
     * @param deploymentUnit
     * @return
     */
    private Set<ClassLoader> allDeploymentModuleClassLoaders(DeploymentUnit deploymentUnit) {
        final DeploymentUnit topDeploymentUnit = DeploymentUtils.getTopDeploymentUnit(deploymentUnit);
        final Module toplevelModule = topDeploymentUnit.getAttachment(Attachments.MODULE);
        Set<ClassLoader> deploymentClassLoaders = new HashSet<ClassLoader>();

        deploymentClassLoaders.add(toplevelModule.getClassLoader());
        final List<DeploymentUnit> subDeployments = topDeploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        for (DeploymentUnit subDeploymentUnit: subDeployments) {
            final Module subDeploymentModule = subDeploymentUnit.getAttachment(Attachments.MODULE);
            if (subDeploymentModule != null) {
                deploymentClassLoaders.add(subDeploymentModule.getClassLoader());
            }
        }
        return deploymentClassLoaders;
    }
}

