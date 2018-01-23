/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.ra.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.resourceadapters.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class RaXmlDeploymentProcessor implements DeploymentUnitProcessor {

    public RaXmlDeploymentProcessor() {

    }

    /**
     * Process a deployment for a Connector. Will install a {@Code
     * JBossService} for this ResourceAdapter.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ManagementResourceRegistration baseRegistration = deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);
        final ManagementResourceRegistration registration;
        final Resource deploymentResource = phaseContext.getDeploymentUnit().getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        final ConnectorXmlDescriptor connectorXmlDescriptor = deploymentUnit.getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        if (connectorXmlDescriptor == null) {
            return; // Skip non ra deployments
        }
        if (deploymentUnit.getParent() != null) {
            registration = baseRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement("subdeployment")));
        } else {
            registration = baseRegistration;
        }
        ResourceAdaptersService.ModifiableResourceAdaptors raxmls = null;
        final ServiceController<?> raService = phaseContext.getServiceRegistry().getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);
        if (raService != null)
            raxmls = ((ResourceAdaptersService.ModifiableResourceAdaptors) raService.getValue());

        ROOT_LOGGER.tracef("processing Raxml");
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        try {
            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            String deploymentUnitPrefix = "";

            if (deploymentUnit.getParent() != null) {
                deploymentUnitPrefix = deploymentUnit.getParent().getName() + "#";
            }

            final String deploymentUnitName = deploymentUnitPrefix + deploymentUnit.getName();

            if (raxmls != null) {
                for (Activation raxml : raxmls.getActivations()) {

                    String rarName = raxml.getArchive();

                    if (deploymentUnitName.equals(rarName)) {
                        RaServicesFactory.createDeploymentService(registration, connectorXmlDescriptor, module, serviceTarget, deploymentUnitName, deploymentUnit.getServiceName(), deploymentUnitName, raxml, deploymentResource, phaseContext.getServiceRegistry());

                    }
                }
            }

            //create service pointing to rar for other future activations
            ServiceName serviceName = ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(deploymentUnitName);
            InactiveResourceAdapterDeploymentService service = new InactiveResourceAdapterDeploymentService(connectorXmlDescriptor, module, deploymentUnitName, deploymentUnitName, deploymentUnit.getServiceName(), registration, serviceTarget, deploymentResource);
            ServiceBuilder builder = serviceTarget
                    .addService(serviceName, service);
            builder.setInitialMode(Mode.ACTIVE).install();

        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }


}
