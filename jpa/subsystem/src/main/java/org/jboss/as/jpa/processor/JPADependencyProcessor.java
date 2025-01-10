/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Deployment processor which adds a module dependencies for modules needed for Jakarta Persistence deployments.
 *
 * @author Scott Marlow (copied from WeldDepedencyProcessor)
 */
public class JPADependencyProcessor implements DeploymentUnitProcessor {

    private static final String JAVAX_PERSISTENCE_API_ID = "jakarta.persistence.api";
    private static final String JBOSS_AS_JPA_ID = "org.jboss.as.jpa";
    private static final String JBOSS_AS_JPA_SPI_ID = "org.jboss.as.jpa.spi";

    /**
     * Add dependencies for modules required for Jakarta Persistence deployments
     */
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        // all applications get the jakarta.persistence SPEC API module added to their deployment by default
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, JAVAX_PERSISTENCE_API_ID).setImportServices(true).build());
        ROOT_LOGGER.debugf("added %s dependency to %s", JAVAX_PERSISTENCE_API_ID, deploymentUnit.getName());

        if (!JPADeploymentMarker.isJPADeployment(deploymentUnit)) {
            return; // Skip if there are no persistence use in the deployment
        }
        for (String moduleIdentifier : new String[]{JBOSS_AS_JPA_ID, JBOSS_AS_JPA_SPI_ID}) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setImportServices(true).build());
            ROOT_LOGGER.debugf("added %s dependency to %s", moduleIdentifier, deploymentUnit.getName());
        }
        addPersistenceProviderModuleDependencies(phaseContext, moduleSpecification, moduleLoader);
    }

    private void addPersistenceProviderModuleDependencies(DeploymentPhaseContext phaseContext, ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {

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
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, dependency).setImportServices(true).build());
            ROOT_LOGGER.debugf("added %s dependency to %s", dependency, deploymentUnit.getName());
        }

        // add the PU service as a dependency to all EE components in this scope
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> components = eeModuleDescription.getComponentDescriptions();
        boolean earSubDeploymentsAreInitializedInCustomOrder = false;
        EarMetaData earConfig = null;

        earConfig = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
        earSubDeploymentsAreInitializedInCustomOrder = earConfig != null && earConfig.getInitializeInOrder() && earConfig.getModules().size() > 1;
        // WFLY-14923 as per https://jakarta.ee/specifications/platform/8/platform-spec-8.html#a3201,
        // respect the `initialize-in-order` setting by only adding EE component dependencies on
        // persistence units in the same sub-deployment (and top level deployment)
        if (earSubDeploymentsAreInitializedInCustomOrder) {

            if (deploymentUnit.getParent() != null) {
                // add persistence units defined in current (sub) deployment unit to EE components
                // also in current deployment unit.
                List<PersistenceUnitMetadata> collectPersistenceUnitsForCurrentDeploymentUnit = new ArrayList<>();
                final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
                final ModuleMetaData moduleMetaData = deploymentRoot.getAttachment(org.jboss.as.ee.structure.Attachments.MODULE_META_DATA);

                for (PersistenceUnitMetadataHolder holder : persistenceUnitsInApplication.getPersistenceUnitHolders()) {
                    if (holder != null && holder.getPersistenceUnits() != null) {
                        for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
                            String moduleName = pu.getContainingModuleName().get(pu.getContainingModuleName().size() - 1);
                            if (moduleName.equals(moduleMetaData.getFileName())) {
                                ROOT_LOGGER.tracef("Jakarta EE components in %s will depend on persistence unit %s", moduleName, pu.getScopedPersistenceUnitName());
                                collectPersistenceUnitsForCurrentDeploymentUnit.add(pu);
                            }
                        }
                    }
                }
                if (!collectPersistenceUnitsForCurrentDeploymentUnit.isEmpty()) {
                    addPUServiceDependencyToComponents(components,
                            new PersistenceUnitMetadataHolder(collectPersistenceUnitsForCurrentDeploymentUnit));
                }
            } else {
                // WFLY-14923
                // add Jakarta EE component dependencies on all persistence units in top level deployment unit.
                List<ResourceRoot> resourceRoots = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachmentList(Attachments.RESOURCE_ROOTS);
                for (ResourceRoot resourceRoot : resourceRoots) {
                    // look at resources that aren't subdeployments
                    if (!SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                        addPUServiceDependencyToComponents(components, resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS));
                    }
                }
            }
            // end of earSubDeploymentsAreInitializedInCustomOrder handling
        } else {
            // no `initialize-in-order` ordering configuration was specified (this is the default).
            for (PersistenceUnitMetadataHolder holder : persistenceUnitsInApplication.getPersistenceUnitHolders()) {
                addPUServiceDependencyToComponents(components, holder);
            }
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

    private int loadPersistenceUnits(final ModuleSpecification moduleSpecification, final ModuleLoader moduleLoader, final DeploymentUnit deploymentUnit, final Set<String> moduleDependencies, final PersistenceUnitMetadataHolder holder) {
        int defaultProviderCount = 0;
        if (holder != null) {
            for (PersistenceUnitMetadata pu : holder.getPersistenceUnits()) {
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
                    String persistenceProviderModule = Configuration.getProviderModuleNameFromProviderClassName(pu.getPersistenceProviderClassName());
                    if (persistenceProviderModule != null) {
                        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, persistenceProviderModule).setOptional(true).build());
                        ROOT_LOGGER.debugf("Adding %s dependency to %s.  " +
                                "Persistence Unit %s is configured to use Persistence Provider '%s', adding an optional dependency on Persistence Provider Module '%s'",
                                persistenceProviderModule, deploymentUnit.getName(),
                                pu.getPersistenceUnitName(), pu.getPersistenceProviderClassName(), persistenceProviderModule);
                    }
                }
            }
        }
        return defaultProviderCount;
    }
}
