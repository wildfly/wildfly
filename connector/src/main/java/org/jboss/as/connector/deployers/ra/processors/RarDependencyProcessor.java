/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra.processors;

import org.jboss.as.connector.subsystems.resourceadapters.Capabilities;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

public class RarDependencyProcessor implements DeploymentUnitProcessor {

    private static String JMS_ID = "jakarta.jms.api";
    private static String IRON_JACAMAR_ID = "org.jboss.ironjacamar.api";
    private static String IRON_JACAMAR_IMPL_ID = "org.jboss.ironjacamar.impl";
    private static String VALIDATION_ID = "jakarta.validation.api";
    private static String HIBERNATE_VALIDATOR_ID = "org.hibernate.validator";
    private static String RESOURCE_API_ID = "jakarta.resource.api";

    /**
     * Add dependencies for modules required for ra deployments
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, RESOURCE_API_ID, false, false, false, false));
        if (phaseContext.getDeploymentUnit().getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY) == null) {
            return;  // Skip non ra deployments
        }

        //if a module depends on a rar it also needs a dep on all the rar's "local dependencies"
        moduleSpecification.setLocalDependenciesTransitive(true);
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JMS_ID, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, VALIDATION_ID, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, IRON_JACAMAR_ID, false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, IRON_JACAMAR_IMPL_ID, false, true, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, HIBERNATE_VALIDATOR_ID, false, false, true, false));
        if (support.hasCapability(Capabilities.RESOURCE_ADAPTERS_SUBSYSTEM_CAPABILITY_NAME)) {
            phaseContext.addDeploymentDependency(ConnectorServices.RESOURCEADAPTERS_SUBSYSTEM_SERVICE, ResourceAdaptersSubsystemService.ATTACHMENT_KEY);
        }
    }
}
