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

import org.jboss.as.jpa.persistenceprovider.PersistenceProviderAdapterRegistry;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

import javax.persistence.spi.PersistenceProvider;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Deploy JPA Persistence providers that are found in the application deployment.
 * Modeled after DriverProcessor.
 *
 * TODO:  add versioning support so that we could have multiple versions of the provider.
 * Or use the provider jar name (which could include a version) as a version tag that
 * could be specified in the persistence.xml
 *
 * @author Scott Marlow
 */
public class PersistenceProviderProcessor implements DeploymentUnitProcessor {
    private static final Logger log = Logger.getLogger("org.jboss.as.jpa");

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ServicesAttachment servicesAttachment = deploymentUnit.getAttachment(Attachments.SERVICES);
        if (module != null && servicesAttachment != null) {
            final ModuleClassLoader classLoader = module.getClassLoader();
            final List<String> providerNames = servicesAttachment.getServiceImplementations(PersistenceProvider.class.getName());
            for (String providerName : providerNames) {
                try {
                    final Class<? extends PersistenceProvider> providerClass = classLoader.loadClass(providerName).asSubclass(PersistenceProvider.class);
                    final Constructor<? extends PersistenceProvider> constructor = providerClass.getConstructor();
                    final PersistenceProvider provider = constructor.newInstance();
                    log.infof("Deploying Persistence Provider %s ", providerClass);
                    phaseContext
                            .getServiceTarget()
                            .addService(
                                    ServiceName.JBOSS.append("persistenceprovider", providerName ),
                                    new ValueService<PersistenceProvider>(new ImmediateValue<PersistenceProvider>(provider))).setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();
// temp hack for testing  PersistenceProviderResolverImpl.getInstance().clearCachedProviders();
                    PersistenceProviderResolverImpl.getInstance().addPersistenceProvider(provider);

                } catch (Exception e) {
                    log.warnf("Unable to instantiate persistence provider class \"%s\": %s", providerName, e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
    }


}
