/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_BARRIER;
import static org.jboss.as.ejb3.subsystem.MdbDeliveryGroupResourceDefinition.MDB_DELIVERY_GROUP;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ejb3.component.messagedriven.MdbDeliveryControllerService;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.function.Supplier;

/**
 * MdbDeliveryDependencies DUP, creates an MdbDeliveryControllerService to enable/disable delivery according to that MDBs
 * delivery group configuration, and according to whether the Mdb is clustered singleton or not.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryDependenciesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(EE_MODULE_CONFIGURATION);
        if (moduleConfiguration == null) {
            return;
        }
        // support for using capabilities to resolve service names
        CapabilityServiceSupport capabilityServiceSupport = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        boolean clusteredSingletonFound = false;
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                final MessageDrivenComponentDescription mdbDescription = (MessageDrivenComponentDescription) description;
                if (mdbDescription.isDeliveryControlled()) {
                    // Use of named service is required by MessageDrivenBeanRuntimeHandler
                    ServiceBuilder<?> builder = serviceTarget.addService(mdbDescription.getDeliveryControllerName());

                    if (mdbDescription.isClusteredSingleton()) {
                        clusteredSingletonFound = true;
                        // Require clustered singleton
                        builder.requires(capabilityServiceSupport.getCapabilityServiceName(CLUSTERED_SINGLETON));
                    }

                    if (mdbDescription.getDeliveryGroups() != null) {
                        for (String deliveryGroup : mdbDescription.getDeliveryGroups()) {
                            if (!capabilityServiceSupport.hasCapability(MDB_DELIVERY_GROUP, deliveryGroup)) {
                                throw EjbLogger.DEPLOYMENT_LOGGER.missingMdbDeliveryGroup(deliveryGroup);
                            }
                            builder.requires(capabilityServiceSupport.getCapabilityServiceName(MDB_DELIVERY_GROUP, deliveryGroup));
                        }
                    }

                    Supplier<MessageDrivenComponent> component = builder.requires(description.getCreateServiceName());
                    builder.setInstance(new MdbDeliveryControllerService(component))
                            .setInitialMode(ServiceController.Mode.PASSIVE)
                            .install();
                }
            }
        }
        if (clusteredSingletonFound) {
            // Ensure singleton barrier is started
            ServiceInstaller.builder(Boolean.TRUE).requires(ServiceDependency.on(CLUSTERED_SINGLETON_BARRIER)).build().install(phaseContext);
        }
    }
}
