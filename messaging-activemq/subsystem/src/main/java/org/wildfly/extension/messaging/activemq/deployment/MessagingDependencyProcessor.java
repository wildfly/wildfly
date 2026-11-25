/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * Processor that add module dependencies for Jakarta Messaging deployments.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class MessagingDependencyProcessor implements DeploymentUnitProcessor {

    /**
     * We include this module so that the CDI producer method for JMSContext is available for the deployment unit.
     */
    public static final String AS_MESSAGING = "org.wildfly.extension.messaging-activemq.injection";
    public static final String JMS_API = "jakarta.jms.api";
    public static final String JTS = "org.jboss.jts";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        addDependency(moduleSpecification, moduleLoader, JMS_API);

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            final WeldCapability api = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get();
            if (api.isPartOfWeldDeployment(deploymentUnit)) {
                addDependency(moduleSpecification, moduleLoader, AS_MESSAGING);
                // The messaging-activemq subsystem provides support for injected JMSContext.
                // one of the beans has a @TransactionScoped scope which requires the CDI context
                // provided by Narayana in the org.jboss.jts module.
                // @see CDIDeploymentProcessor
                addDependency(moduleSpecification, moduleLoader, JTS);
            }
        }
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader, String moduleIdentifier) {
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setImportServices(true).build());
    }
}
