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

import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.server.deployment.Attachable;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

import javax.persistence.spi.PersistenceProvider;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Deploy JPA Persistence providers that are found in the application deployment.
 *
 * @author Scott Marlow
 */
public class PersistenceProviderProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.as.jpa");

    /**
     * {@inheritDoc}
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        final ServicesAttachment servicesAttachment = deploymentUnit.getAttachment(Attachments.SERVICES);
        if (module != null && servicesAttachment != null) {
            final ModuleClassLoader classLoader = module.getClassLoader();
            PersistenceProvider provider = null;
            // collect list of persistence providers packaged with the application
            final List<String> providerNames = servicesAttachment.getServiceImplementations(PersistenceProvider.class.getName());
            if (providerNames.size() > 1) {     // TODO: support more than one provider to be packaged, which requires
                                                // knowing which adapter belongs with it.
                throw new DeploymentUnitProcessingException(
                    "only one persistence provider can be packaged with an application " + providerNames);
            }
            for (String providerName : providerNames) {
                try {
                    final Class<? extends PersistenceProvider> providerClass = classLoader.loadClass(providerName).asSubclass(PersistenceProvider.class);
                    final Constructor<? extends PersistenceProvider> constructor = providerClass.getConstructor();
                    provider = constructor.newInstance();
                    log.infof("Deployment has its own Persistence Provider %s ", providerClass);

                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not deploy application packaged persistence provider '" + providerName+"'", e);
                }
            }

            PersistenceProviderDeploymentHolder holder;
            Attachable topDu = top(deploymentUnit);
            holder = topDu.getAttachment(PersistenceProviderDeploymentHolder.DEPLOYED_PERSISTENCE_PROVIDER);
            if(provider != null) {

                if(holder == null) {
                    holder = new PersistenceProviderDeploymentHolder();
                }
                holder.setProvider(provider);
                String adapterClass = holder.getPersistenceProviderAdaptorClassName();
                if (adapterClass != null) {
                    try {
                        PersistenceProviderAdaptor adaptor = (PersistenceProviderAdaptor)classLoader.loadClass(adapterClass).newInstance();
                        holder.setAdapter(adaptor);
                        adaptor.setJtaManager(JtaManagerImpl.getInstance());
                    } catch (InstantiationException e) {
                        throw new DeploymentUnitProcessingException("could not create instance of adapter class '" +
                            adapterClass +"'", e);
                    } catch (IllegalAccessException e) {
                        throw new DeploymentUnitProcessingException("could not create instance of adapter class '" +
                            adapterClass +"'", e);
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException("could not create instance of adapter class '" +
                            adapterClass +"'", e);
                    }
                }
                topDu.putAttachment(PersistenceProviderDeploymentHolder.DEPLOYED_PERSISTENCE_PROVIDER, holder);
            }
        }
    }

    // save in consistent location (top) for all deployments.
    private Attachable top(DeploymentUnit deploymentUnit) {
        while (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        return deploymentUnit;
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(final DeploymentUnit context) {
    }


}
