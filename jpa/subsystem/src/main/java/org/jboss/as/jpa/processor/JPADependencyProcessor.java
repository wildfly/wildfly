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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.config.PersistenceUnitsInApplication;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Deployment processor which adds a module dependencies for modules needed for JPA deployments.
 *
 * @author Scott Marlow (copied from WeldDepedencyProcessor)
 */
public class JPADependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier JAVAX_PERSISTENCE_API_ID = ModuleIdentifier.create("javax.persistence.api");
    private static final ModuleIdentifier JBOSS_AS_JPA_ID = ModuleIdentifier.create("org.jboss.as.jpa");
    private static final ModuleIdentifier JBOSS_AS_JPA_SPI_ID = ModuleIdentifier.create("org.jboss.as.jpa.spi");
    // HIBERNATE_TRANSFORMER_ID will be removed in a future WildFly release.
    private static final ModuleIdentifier HIBERNATE_TRANSFORMER_ID = ModuleIdentifier.create("org.hibernate.bytecodetransformer");

    /**
     * Add dependencies for modules required for JPA deployments
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        // all applications get the javax.persistence module added to their deplyoment by default
        addDependency(moduleSpecification, moduleLoader, deploymentUnit, JAVAX_PERSISTENCE_API_ID, HIBERNATE_TRANSFORMER_ID);

        if (!JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            return; // Skip if there are no persistence use in the deployment
        }
        addDependency(moduleSpecification, moduleLoader, deploymentUnit, JBOSS_AS_JPA_ID, JBOSS_AS_JPA_SPI_ID);
        addPersistenceProviderModuleDependencies(phaseContext, moduleSpecification, moduleLoader);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               DeploymentUnit deploymentUnit, ModuleIdentifier... moduleIdentifiers) {
        for ( ModuleIdentifier moduleIdentifier : moduleIdentifiers) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, true, false));
            ROOT_LOGGER.debugf("added %s dependency to %s", moduleIdentifier, deploymentUnit.getName());
        }
    }

    private void addOptionalDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               DeploymentUnit deploymentUnit, ModuleIdentifier... moduleIdentifiers) {
        for ( ModuleIdentifier moduleIdentifier : moduleIdentifiers) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, true, false, false, false));
            ROOT_LOGGER.debugf("added %s dependency to %s", moduleIdentifier, deploymentUnit.getName());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private void addPersistenceProviderModuleDependencies(DeploymentPhaseContext phaseContext, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) throws
        DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        int defaultProviderCount = 0;
        Set<String> moduleDependencies = new HashSet<String>();

        // get the number of persistence units that use the default persistence provider module.
        // Dependencies for other persistence provider will be added to the passed
        // 'moduleDependencies' collection.  Each persistence provider module that is found, will be injected into the
        // passed moduleSpecification (for the current deployment unit).
        PersistenceUnitsInApplication persistenceUnitsInApplication = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(PersistenceUnitsInApplication.PERSISTENCE_UNITS_IN_APPLICATION);
        for (PersistenceUnitMetadataHolder holder: persistenceUnitsInApplication.getPersistenceUnitHolders()) {
            defaultProviderCount += loadPersistenceUnits(moduleSpecification, moduleLoader, deploymentUnit, moduleDependencies, holder);
        }

        // add dependencies for the default persistence provider module
        if (defaultProviderCount > 0) {
            moduleDependencies.add(Configuration.getDefaultProviderModuleName());
            ROOT_LOGGER.debugf("added (default provider) %s dependency to %s (since %d PU(s) didn't specify %s",
                Configuration.getDefaultProviderModuleName(), deploymentUnit.getName(),defaultProviderCount, Configuration.PROVIDER_MODULE + ")");
        }

        // add persistence provider dependency
        for (String dependency : moduleDependencies) {
            addDependency(moduleSpecification, moduleLoader, deploymentUnit, ModuleIdentifier.fromString(dependency));
        }

        // add the PU service as a dependency to all EE components in this scope
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> components = eeModuleDescription.getComponentDescriptions();
        for (PersistenceUnitMetadataHolder holder: persistenceUnitsInApplication.getPersistenceUnitHolders()) {
            addPUServiceDependencyToComponents(components, holder);
        }

    }

    /**
     * Add the <code>puServiceName</code> as a dependency on each of the passed <code>components</code>
     *
     * @param components    The components to which the PU service is added as a dependency
     * @param holder        The persistence units
     */
    private static void addPUServiceDependencyToComponents(final Collection<ComponentDescription> components, final PersistenceUnitMetadataHolder holder) {

        if (components == null || components.isEmpty() || holder == null) {
            return;
        }

        for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
            String jpaContainerManaged = pu.getProperties().getProperty(Configuration.JPA_CONTAINER_MANAGED);
            boolean deployPU = (jpaContainerManaged == null? true : Boolean.parseBoolean(jpaContainerManaged));
            if (deployPU) {
                final ServiceName puServiceName = PersistenceUnitServiceImpl.getPUServiceName(pu);
                for (final ComponentDescription component : components) {
                    ROOT_LOGGER.debugf("Adding dependency on PU service %s for component %s", puServiceName, component.getComponentClassName());
                    component.addDependency(puServiceName);
                }
            }
        }
    }

    private int loadPersistenceUnits(final ModuleSpecification moduleSpecification, final ModuleLoader moduleLoader, final DeploymentUnit deploymentUnit, final Set<String> moduleDependencies, final PersistenceUnitMetadataHolder holder) throws
        DeploymentUnitProcessingException {
        int defaultProviderCount = 0;
        if (holder != null) {
            for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                String providerModule = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
                String adapterModule = pu.getProperties().getProperty(Configuration.ADAPTER_MODULE);
                String adapterClass = pu.getProperties().getProperty(Configuration.ADAPTER_CLASS);

                if (adapterModule != null) {
                    ROOT_LOGGER.debugf("%s is configured to use adapter module '%s'", pu.getPersistenceUnitName(), adapterModule);
                    moduleDependencies.add(adapterModule);
                }
                deploymentUnit.putAttachment(JpaAttachments.ADAPTOR_CLASS_NAME, adapterClass);

                String provider = pu.getProperties().getProperty(Configuration.PROVIDER_MODULE);
                if (provider != null) {
                    if (provider.equals(Configuration.PROVIDER_MODULE_APPLICATION_SUPPLIED)) {
                        ROOT_LOGGER.debugf("%s is configured to use application supplied persistence provider", pu.getPersistenceUnitName());
                    } else {
                        moduleDependencies.add(provider);
                        ROOT_LOGGER.debugf("%s is configured to use provider module '%s'", pu.getPersistenceUnitName(), provider);
                    }
                } else if (Configuration.PROVIDER_CLASS_DEFAULT.equals(pu.getPersistenceProviderClassName())) {
                    defaultProviderCount++;  // track number of references to default provider module
                } else {
                    // inject other provider modules into application
                    // in case its not obvious, everything but hibernate3 can end up here.  For Hibernate3, the Configuration.PROVIDER_MODULE
                    // should of been specified.
                    //
                    // since we don't know (until after PersistenceProviderProcessor runs in a later phase) if the provider
                    // is packaged with the app or will be accessed as a module, make the module dependency optional (in case it
                    // doesn't exist).
                    String providerModuleName = Configuration.getProviderModuleNameFromProviderClassName(pu.getPersistenceProviderClassName());
                    if (providerModuleName != null) {
                        addOptionalDependency(moduleSpecification, moduleLoader, deploymentUnit, ModuleIdentifier.fromString(providerModuleName));
                        ROOT_LOGGER.debugf("%s is configured to use persistence provider '%s', adding an optional dependency on module '%s'",
                                pu.getPersistenceUnitName(), pu.getPersistenceProviderClassName(), providerModuleName);
                    }
                }
            }
        }
        return defaultProviderCount;
    }
}
