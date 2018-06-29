/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ejb3.clustering.SingletonBarrierService;
import org.jboss.as.ejb3.component.messagedriven.MdbDeliveryControllerService;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.MdbDeliveryGroupResourceDefinition;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;

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
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        boolean clusteredSingletonFound = false;
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                final MessageDrivenComponentDescription mdbDescription = (MessageDrivenComponentDescription) description;
                if (mdbDescription.isDeliveryControlled()) {
                    final MdbDeliveryControllerService mdbDeliveryControllerService = new MdbDeliveryControllerService();
                    final ServiceBuilder<MdbDeliveryControllerService> builder = serviceTarget
                            .addService(mdbDescription.getDeliveryControllerName(), mdbDeliveryControllerService)
                            .addDependency(description.getCreateServiceName(), MessageDrivenComponent.class,
                                    mdbDeliveryControllerService.getMdbComponent())
                            .setInitialMode(Mode.PASSIVE);
                    if (mdbDescription.isClusteredSingleton()) {
                        clusteredSingletonFound = true;
                        builder.addDependency(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName());
                    }
                    if (mdbDescription.getDeliveryGroup() != null) {
                        final ServiceName deliveryGroupServiceName = MdbDeliveryGroupResourceDefinition.getDeliveryGroupServiceName(
                                mdbDescription.getDeliveryGroup());
                        if (phaseContext.getServiceRegistry().getService(deliveryGroupServiceName) == null) {
                            throw EjbLogger.DEPLOYMENT_LOGGER.missingMdbDeliveryGroup(mdbDescription.getDeliveryGroup());
                        }
                        builder.addDependency(deliveryGroupServiceName);
                    }
                    builder.install();
                }
            }
        }
        if (clusteredSingletonFound) {
            // Add dependency on the singleton barrier, which starts on-demand
            // (the MDB delivery controller won't demand the singleton barrier service to start)
            serviceTarget.addDependency(SingletonBarrierService.SERVICE_NAME);
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
                if (mdbDescription.isClusteredSingleton() || mdbDescription.getDeliveryGroup() != null) {
                    serviceRegistry.getRequiredService(mdbDescription.getDeliveryControllerName()).setMode(Mode.REMOVE);
                }
            }
        }
    }
}
