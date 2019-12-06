/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.openapi.deployment;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * Processes the Open API model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {

    private static final String ENABLED = "mp.openapi.extensions.enabled";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (unit.getAttachment(OpenAPIDependencyProcessor.ATTACHMENT_KEY).booleanValue()) {
            Config config = ConfigProvider.getConfig(unit.getAttachment(Attachments.MODULE).getClassLoader());

            if (!config.getOptionalValue(ENABLED, Boolean.class).orElse(Boolean.TRUE).booleanValue()) {
                MicroProfileOpenAPILogger.LOGGER.disabled(unit.getName());
                return;
            }

            // Fetch server/host as determined by Undertow DUP
            ModelNode model = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
            String serverName = model.get(DeploymentDefinition.SERVER.getName()).asString();
            String hostName = model.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();

            ServiceTarget target = context.getServiceTarget();
            OpenAPIModelServiceConfigurator configurator = new OpenAPIModelServiceConfigurator(unit, serverName, hostName, config);
            ServiceName modelServiceName = configurator.getServiceName();

            try {
                // Only one deployment can register the same OpenAPI endpoint with a given host
                // Let the first one to register win
                if (context.getServiceRegistry().getService(modelServiceName) != null) {
                    throw new DuplicateServiceException(modelServiceName.getCanonicalName());
                }

                configurator.build(target).install();

                new OpenAPIHttpHandlerServiceConfigurator(configurator).build(target).install();
            } catch (DuplicateServiceException e) {
                MicroProfileOpenAPILogger.LOGGER.endpointAlreadyRegistered(hostName, unit.getName());
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
    }
}
