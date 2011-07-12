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

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceProviderDeploymentHolder;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderAdapterRegistry;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.transaction.JtaManagerImpl;
import org.jboss.as.server.deployment.Attachable;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Deployment processor which adds a module dependencies for modules needed for JPA deployments.
 *
 * @author Scott Marlow (copied from WeldDepedencyProcessor)
 */
public class JPADependencyProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.as.jpa");

    private static ModuleIdentifier JAVAX_PERSISTENCE_API_ID = ModuleIdentifier.create("javax.persistence.api");
    private static ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");
    private static ModuleIdentifier JBOSS_AS_JPA_ID = ModuleIdentifier.create("org.jboss.as.jpa");
    private static ModuleIdentifier JBOSS_AS_JPA_SPI_ID = ModuleIdentifier.create("org.jboss.as.jpa.spi");
    private static ModuleIdentifier JAVASSIST_ID = ModuleIdentifier.create("org.javassist");


    /**
     * Add dependencies for modules required for JPA deployments
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDependency(moduleSpecification, moduleLoader, JAVAX_PERSISTENCE_API_ID);

        if (!JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            log.infof("added javax.persistence.api dependency to %s", deploymentUnit.getName());
            return; // Skip if there are no persistence use in the deployment
        }
        addDependency(moduleSpecification, moduleLoader, JAVAEE_API_ID);
        addDependency(moduleSpecification, moduleLoader, JBOSS_AS_JPA_ID);
        addDependency(moduleSpecification, moduleLoader, JBOSS_AS_JPA_SPI_ID);  // cover when adapter jar is in app
        addDependency(moduleSpecification, moduleLoader, JAVASSIST_ID);
        log.infof("added javax.persistence.api, javaee.api, org.jboss.as.jpa, org.javassist dependencies to %s",deploymentUnit.getName());
        addPersistenceProviderModuleDependencies(phaseContext, moduleSpecification, moduleLoader);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier) {
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }


    private void addPersistenceProviderModuleDependencies(DeploymentPhaseContext phaseContext, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) throws
        DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        int defaultProviderCount = 0;
        HashMap<String, Object> providers = new HashMap<String, Object>();
        PersistenceUnitMetadataHolder holder = deploymentRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);

        if (holder != null) {
            for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                String adapterModule = pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
                String adapterClass = pu.getProperties().getProperty(Configuration.ADAPTER_CLASS);
                if (adapterModule!= null && adapterClass != null) {
                    log.info(pu.getPersistenceUnitName() +
                        " is configured to use adapter module '" +adapterModule+"' and adapter class '" + adapterClass+"'");

                    String persistenceProvider = pu.getPersistenceProviderClassName() != null
                        ? pu.getPersistenceProviderClassName() : Configuration.PROVIDER_CLASS_DEFAULT;
                    // load persistence provider adapter if not loaded yet
                    loadPersistenceAdapterModule(moduleLoader, adapterModule, persistenceProvider);
                }
                else if (adapterClass != null) {    // adapter class is expected to be in deployment
                    savePersistenceAdapterClass(deploymentUnit, adapterClass);
                }

                String provider = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
                if (provider != null) {
                    if (provider.equals(Configuration.PROVIDER_MODULE_APPLICATION_SUPPLIED)) {
                        log.info(pu.getPersistenceUnitName() +
                            " is configured to use application supplied persistence provider");
                    }
                    else {
                        providers.put(provider, null);
                    }
                }
                else {
                    defaultProviderCount++;  // track number of references to default provider module
                }
            }
        }

        List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        assert resourceRoots != null;
        for (ResourceRoot resourceRoot : resourceRoots) {
            holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
            if (holder != null) {
                for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                    String adapterModule =pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
                    String adapterClass = pu.getProperties().getProperty(Configuration.ADAPTER_CLASS);
                    if (adapterModule!= null && adapterClass != null) {
                        log.info(pu.getPersistenceUnitName() +
                            " is configured to use adapter module '" +adapterModule+"' and adapter class '" + adapterClass+"'");

                        String persistenceProvider = pu.getPersistenceProviderClassName() != null
                            ? pu.getPersistenceProviderClassName() : Configuration.PROVIDER_CLASS_DEFAULT;
                        // load persistence provider adapter if not loaded yet
                        loadPersistenceAdapterModule(moduleLoader, adapterModule, persistenceProvider);
                    }
                    else if (adapterClass != null) {    // adapter class is expected to be in deployment
                        savePersistenceAdapterClass(deploymentUnit, adapterClass);
                    }

                    String provider = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
                    if (provider != null) {
                        if (provider.equals(Configuration.PROVIDER_MODULE_APPLICATION_SUPPLIED)) {
                            log.info(pu.getPersistenceUnitName() +
                                " is configured to use application supplied persistence provider");
                        }
                        else {
                            providers.put(provider, null);
                        }
                    }
                    else {
                        defaultProviderCount++;  // track number of references to default provider module
                    }
                }
            }
        }
        // add dependencies for the default persistence provider module
        if (defaultProviderCount > 0 ) {
            providers.put(Configuration.PROVIDER_MODULE_DEFAULT, null);
            log.info("added (default provider) "+ Configuration.PROVIDER_MODULE_DEFAULT +
                " dependency to application deployment (since "+defaultProviderCount+" PU(s) didn't specify "+Configuration.PROVIDER_MODULE+")");
        }

        // add persistence provider dependency
        for (String dependency : providers.keySet()) {
            addDependency(moduleSpecification, moduleLoader, ModuleIdentifier.create(dependency));
            log.info("added "+ dependency + " dependency to application deployment");
        }
    }

    // adapter class is in deployment
    private void savePersistenceAdapterClass(DeploymentUnit deploymentUnit, String adapterClass) {
        Attachable topDu = top(deploymentUnit);
        PersistenceProviderDeploymentHolder holder;
        holder = topDu.getAttachment(PersistenceProviderDeploymentHolder.DEPLOYED_PERSISTENCE_PROVIDER);
        if ( holder == null) {
            holder = new PersistenceProviderDeploymentHolder();
        }
        holder.setPersistenceProviderAdaptorClassName(adapterClass);
        topDu.putAttachment(PersistenceProviderDeploymentHolder.DEPLOYED_PERSISTENCE_PROVIDER, holder);
    }

    // save in consistent location (top) for all deployments.
    private Attachable top(DeploymentUnit deploymentUnit) {
        while (deploymentUnit.getParent() != null) {
            deploymentUnit = deploymentUnit.getParent();
        }
        return deploymentUnit;
    }


    private void loadPersistenceAdapterModule(ModuleLoader moduleLoader, String adapterModule, String persistenceProviderClass) throws
        DeploymentUnitProcessingException {

        if (PersistenceProviderAdapterRegistry.getPersistenceProviderAdaptor(persistenceProviderClass, adapterModule) == null) {
            try {
                Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(adapterModule));
                final ServiceLoader<PersistenceProviderAdaptor> serviceLoader =
                    module.loadService(PersistenceProviderAdaptor.class);
                if (serviceLoader != null) {
                    PersistenceProviderAdaptor persistenceProviderAdaptor = null;
                    for(PersistenceProviderAdaptor adaptor: serviceLoader) {
                        if (persistenceProviderAdaptor != null) {
                            throw new DeploymentUnitProcessingException(
                                "persistence provider adapter module has more than one adapters"
                                + adapterModule +"(class "+persistenceProviderClass+")");
                        }
                        persistenceProviderAdaptor = adaptor;
                    }
                    if (persistenceProviderAdaptor != null) {
                        persistenceProviderAdaptor.setJtaManager(JtaManagerImpl.getInstance());
                        PersistenceProviderAdapterRegistry.putPersistenceProviderAdaptor(persistenceProviderClass, adapterModule, persistenceProviderAdaptor);
                    }
                }
            } catch (ModuleLoadException e) {
                throw new DeploymentUnitProcessingException("persistence provider adapter module load error"
                    + adapterModule +"(class "+persistenceProviderClass+")",e);
            }
        }
    }
}
