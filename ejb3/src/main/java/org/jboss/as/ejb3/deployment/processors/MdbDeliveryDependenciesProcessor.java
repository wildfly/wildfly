/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

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
import org.jboss.msc.service.ServiceController.Mode;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.MdbDeliveryGroupResourceDefinition.MDB_DELIVERY_GROUP_CAPABILITY_NAME;

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
                    final MdbDeliveryControllerService mdbDeliveryControllerService = new MdbDeliveryControllerService();
                    final ServiceBuilder<MdbDeliveryControllerService> builder = serviceTarget.addService(mdbDescription.getDeliveryControllerName(), mdbDeliveryControllerService)
                            .addDependency(description.getCreateServiceName(), MessageDrivenComponent.class, mdbDeliveryControllerService.getMdbComponent())
                            .setInitialMode(Mode.PASSIVE);
                    if (mdbDescription.isClusteredSingleton()) {
                        clusteredSingletonFound = true;
                        builder.requires(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName());
                    }
                    if (mdbDescription.getDeliveryGroups() != null) {
                        for (String deliveryGroup : mdbDescription.getDeliveryGroups()) {
                            final ServiceName deliveryGroupServiceName = capabilityServiceSupport.getCapabilityServiceName(MDB_DELIVERY_GROUP_CAPABILITY_NAME, deliveryGroup);
                            if (phaseContext.getServiceRegistry().getService(deliveryGroupServiceName) == null) {
                                throw EjbLogger.DEPLOYMENT_LOGGER.missingMdbDeliveryGroup(deliveryGroup);
                            }
                            builder.requires(deliveryGroupServiceName);
                        }
                    }
                    builder.install();
                }
            }
        }
        if (clusteredSingletonFound) {
            // Add dependency on the default policy, which allows CLUSTERED_SINGLETON_CAPABILITY to be installed
            serviceTarget.addDependency(capabilityServiceSupport.getCapabilityServiceName(ServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR));
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(EE_MODULE_CONFIGURATION);
        if (moduleConfiguration == null) {
            return;
        }
        final ServiceRegistry serviceRegistry = deploymentUnit.getServiceRegistry();
        boolean clusteredSingletonFound = false;
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            final ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                MessageDrivenComponentDescription mdbDescription = (MessageDrivenComponentDescription) description;
                clusteredSingletonFound = clusteredSingletonFound || mdbDescription.isClusteredSingleton();
                if (mdbDescription.isDeliveryControlled()) {
                    serviceRegistry.getRequiredService(mdbDescription.getDeliveryControllerName()).setMode(Mode.REMOVE);
                }
            }
        }
    }
}
