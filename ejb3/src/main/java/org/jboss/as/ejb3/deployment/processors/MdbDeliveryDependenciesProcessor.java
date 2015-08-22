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
import org.jboss.as.ejb3.component.messagedriven.MdbDeliveryControllerService;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.MdbDeliveryGroupResourceDefinition;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.Service;
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
                final String deliveryGroup = mdbDescription.getDeliveryGroup();
                final boolean clusteredSingleton = mdbDescription.isClusteredSingleton();
                if (deliveryGroup != null || clusteredSingleton) {
                    final MdbDeliveryControllerService mdbDeliveryControllerService = new MdbDeliveryControllerService();
                    final ServiceName mdbDeliveryControllerServiceName = createMdbDeliveryControllerServiceName(mdbDescription);
                    final ServiceBuilder<MdbDeliveryControllerService> builder = serviceTarget
                            .addService(mdbDeliveryControllerServiceName, mdbDeliveryControllerService)
                            .addDependency(description.getCreateServiceName(), MessageDrivenComponent.class,
                                    mdbDeliveryControllerService.getMdbComponent())
                            .setInitialMode(Mode.PASSIVE);
                    if (clusteredSingleton) {
                        clusteredSingletonFound = true;
                        builder.addDependency(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName().append("service"));
                    }
                    if (deliveryGroup != null) {
                        final ServiceName deliveyGroupServiceName = MdbDeliveryGroupResourceDefinition.getDeliveryGroupServiceName(
                                deliveryGroup);
                        if (phaseContext.getServiceRegistry().getService(deliveyGroupServiceName) == null) {
                            throw EjbLogger.DEPLOYMENT_LOGGER.missingMdbDeliveryGroup(deliveryGroup);
                        }
                        builder.addDependency(deliveyGroupServiceName);
                    }
                    builder.install();
                }
            }
        }
        if (clusteredSingletonFound) {
            // trigger the start of the clustered singleton capability, since our service above is PASSIVE, and not ACTIVE
            // (the MDB delivery controller won't demand the clustered singleton service to start)
            serviceTarget.addService(createClusteredSingletonDemanderServiceName(deploymentUnit), Service.NULL)
                    .addDependency(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName()).install();
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
                    serviceRegistry.getRequiredService(createMdbDeliveryControllerServiceName(mdbDescription))
                            .setMode(Mode.REMOVE);
                }
            }
        }
        if (clusteredSingletonFound) {
            serviceRegistry.getRequiredService(createClusteredSingletonDemanderServiceName(deploymentUnit))
                    .setMode(Mode.REMOVE);
        }
    }

    private ServiceName createMdbDeliveryControllerServiceName(MessageDrivenComponentDescription mdbDescription) {
        return mdbDescription.getStartServiceName().append("MDB_DELIVERY");
    }

    private ServiceName createClusteredSingletonDemanderServiceName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("clustered", "singleton", "dependency");
    }
}
