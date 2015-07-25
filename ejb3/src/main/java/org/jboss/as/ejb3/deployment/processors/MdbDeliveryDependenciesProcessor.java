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
import org.jboss.as.ejb3.clustering.EJBBoundClusteringMetaData;
import org.jboss.as.ejb3.component.messagedriven.MdbDeliveryControllerService;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.MdbDeliveryGroupResourceDefinition;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

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
        final List<EJBBoundClusteringMetaData> clusteringMetaData = getEJBBoundClusteringMetaData(phaseContext.getDeploymentUnit());
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        boolean clusteredSingletonFound = false;
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                final MessageDrivenComponentDescription mdbDescription = (MessageDrivenComponentDescription) description;
                final String deliveryGroup = mdbDescription.getDeliveryGroup();
                final boolean clusteredSingleton = isClusteredSingleton(mdbDescription, clusteringMetaData);
                if (deliveryGroup != null || clusteredSingleton) {
                    final ServiceName serviceName = description.getStartServiceName();
                    final MdbDeliveryControllerService mdbDeliveryControllerService = new MdbDeliveryControllerService();
                    final ServiceName mdbDeliveryControllerServiceName = createMdbDeliveryControllerServiceName(serviceName);
                    ServiceBuilder<MdbDeliveryControllerService> builder = serviceTarget
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
            // trigger the start of the singleton barrier capability, since our service above is PASSIVE, and not ACTIVE
            // (the MDB delivery controller won' t demand the clustered singleton service to start)
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
        for (final ComponentConfiguration configuration : moduleConfiguration.getComponentConfigurations()) {
            final ComponentDescription description = configuration.getComponentDescription();
            if (description instanceof MessageDrivenComponentDescription) {
                final ServiceName mdbDeliveryControllerServiceName = createMdbDeliveryControllerServiceName(
                        description.getStartServiceName());
                final ServiceController<?> mdbDeliveryControllerService = serviceRegistry.getService(
                        mdbDeliveryControllerServiceName);
                if (mdbDeliveryControllerService != null) {
                    mdbDeliveryControllerService.setMode(Mode.REMOVE);
                }
            }
        }
        if (getEJBBoundClusteringMetaData(deploymentUnit) != null) {
            final ServiceController<?> barrierDependentService = serviceRegistry
                    .getService(createClusteredSingletonDemanderServiceName(deploymentUnit));
            if (barrierDependentService != null) {
                 barrierDependentService.setMode(Mode.REMOVE);
            }
        }
    }

    private ServiceName createMdbDeliveryControllerServiceName(ServiceName mdbComponentName) {
        return mdbComponentName.append("MDB_DELIVERY");
    }

    private ServiceName createClusteredSingletonDemanderServiceName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append("clustered", "singleton", "dependency");
    }

    private List<EJBBoundClusteringMetaData> getEJBBoundClusteringMetaData(DeploymentUnit deploymentUnit) {
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return null;
        }
        final AssemblyDescriptorMetaData assemblyDescriptorMetaData = ejbJarMetaData.getAssemblyDescriptor();
        if (assemblyDescriptorMetaData == null) {
            return null;
        }
        // get the list of clustering meta data
        return assemblyDescriptorMetaData.getAny(EJBBoundClusteringMetaData.class);
    }


    private boolean isClusteredSingleton(final MessageDrivenComponentDescription componentConfiguration, final List<EJBBoundClusteringMetaData> clusteringMetaData) throws DeploymentUnitProcessingException {
        if (clusteringMetaData == null || clusteringMetaData.isEmpty()) {
            return false;
        }
        final String ejbName = componentConfiguration.getEJBName();
        boolean wildcardClusteredSingleton = false;
        for (final EJBBoundClusteringMetaData clusteringMD : clusteringMetaData) {
            final String clusteringEjbName = clusteringMD.getEjbName();
            if (clusteringEjbName.equals("*") && clusteringMD.isClusteredSingleton()) {
                wildcardClusteredSingleton = true;
            }
            if (clusteringEjbName.equals(ejbName) && clusteringMD.isClusteredSingleton()) {
                return true;
            }
        }
        return  wildcardClusteredSingleton;
    }
}
