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

package org.jboss.as.connector.deployers.processors;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.deployment.InactiveResourceAdapterDeploymentService;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersService;
import org.jboss.as.connector.util.RaServicesFactory;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
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

    private final MetadataRepository mdr;

    public RaXmlDeploymentProcessor(final MetadataRepository mdr) {
        this.mdr = mdr;
    }

    /**
     * Process a deployment for a Connector. Will install a {@Code
     * JBossService} for this ResourceAdapter.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ManagementResourceRegistration registration = deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);

        final ConnectorXmlDescriptor connectorXmlDescriptor = deploymentUnit
                .getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        if (connectorXmlDescriptor == null) {
            return; // Skip non ra deployments
        }

        ResourceAdaptersService.ModifiableResourceAdaptors raxmls = null;
        final ServiceController<?> raService = phaseContext.getServiceRegistry().getService(
                ConnectorServices.RESOURCEADAPTERS_SERVICE);
        if (raService != null)
            raxmls = ((ResourceAdaptersService.ModifiableResourceAdaptors) raService.getValue());


        ROOT_LOGGER.tracef("processing Raxml");
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (module == null)
            throw MESSAGES.failedToGetModuleAttachment(deploymentUnit);

        try {
            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            String deploymentUnitPrefix = "";

            if (deploymentUnit.getParent() != null) {
                deploymentUnitPrefix = deploymentUnit.getParent().getName() + "#";
            }

            final String deploymentUnitName = deploymentUnitPrefix + deploymentUnit.getName();

            final String deployment;
            if (deploymentUnitName.lastIndexOf('.') == -1) {
                deployment = deploymentUnitName;
            } else {
                deployment = deploymentUnitName.substring(0, deploymentUnitName.lastIndexOf('.'));
            }
            if (raxmls != null) {
                for (ResourceAdapter raxml : raxmls.getResourceAdapters()) {

                    String rarName = raxml.getArchive();
                    Integer identifier = null;
                    if (rarName.contains(ConnectorServices.RA_SERVICE_NAME_SEPARATOR)) {
                        rarName = rarName.substring(0, rarName.indexOf(ConnectorServices.RA_SERVICE_NAME_SEPARATOR));
                    }
                    if (deploymentUnitName.equals(rarName)) {
                        RaServicesFactory.createDeploymentService(registration, connectorXmlDescriptor, module, serviceTarget, deploymentUnitName, deployment, raxml);


                    }
                }
            }

            //create service pointing to rar for other future activations
            ServiceName serviceName = ConnectorServices.INACTIVE_RESOURCE_ADAPTER_SERVICE.append(deploymentUnitName);

            InactiveResourceAdapterDeploymentService service = new InactiveResourceAdapterDeploymentService(connectorXmlDescriptor, module, deployment, deploymentUnitName, registration);
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
