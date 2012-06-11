/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.appclient.subsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.appclient.deployment.ActiveApplicationClientProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientDependencyProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientDescriptorMethodProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientManifestProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientParsingDeploymentProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientStartProcessor;
import org.jboss.as.appclient.deployment.ApplicationClientStructureProcessor;
import org.jboss.as.appclient.service.ApplicationClientDeploymentService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.appclient.subsystem.Constants.CONNECTION_PROPERTIES_URL;
import static org.jboss.as.appclient.subsystem.Constants.HOST_URL;

/**
 * Add operation handler for the application client subsystem.
 *
 * @author Stuart Douglas
 */
class AppClientSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final AppClientSubsystemAdd INSTANCE = new AppClientSubsystemAdd();

    private final String[] EMPTY_STRING = new String[0];

    private AppClientSubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for(SimpleAttributeDefinition attr : AppClientSubsystemResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String deployment = AppClientSubsystemResourceDefinition.DEPLOYMENT.resolveModelAttribute(context, model).asString();
        final File file = new File(AppClientSubsystemResourceDefinition.FILE.resolveModelAttribute(context, model).asString());
        final String hostUrl = model.hasDefined(HOST_URL) ? AppClientSubsystemResourceDefinition.HOST_URL.resolveModelAttribute(context, model).asString() : null;
        final String connectionPropertiesUrl = model.hasDefined(CONNECTION_PROPERTIES_URL) ? AppClientSubsystemResourceDefinition.CONNECTION_PROPERTIES_URL.resolveModelAttribute(context, model).asString() : null;
        final List<String> parameters = new ArrayList<String>();
        for (ModelNode param : AppClientSubsystemResourceDefinition.PARAMETERS.resolveModelAttribute(context, model).asList()) {
            parameters.add(param.asString());
        }

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                if (deployment != null && !deployment.isEmpty()) {
                    processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_APP_CLIENT, new ApplicationClientStructureProcessor(deployment));
                }
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_APP_CLIENT_XML, new ApplicationClientParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_APPLICATION_CLIENT_MANIFEST, new ApplicationClientManifestProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_APPLICATION_CLIENT_ACTIVE, new ActiveApplicationClientProcessor(deployment));
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_APP_CLIENT_METHOD_RESOLUTION, new ApplicationClientDescriptorMethodProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_APPLICATION_CLIENT, new ApplicationClientDependencyProcessor());
                processorTarget.addDeploymentProcessor(Constants.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_APPLICATION_CLIENT, new ApplicationClientStartProcessor(hostUrl, connectionPropertiesUrl, parameters.toArray(EMPTY_STRING)));

            }
        }, OperationContext.Stage.RUNTIME);

        final ApplicationClientDeploymentService service = new ApplicationClientDeploymentService(file);

        newControllers.add(
                context.getServiceTarget().addService(ApplicationClientDeploymentService.SERVICE_NAME, service)
                        .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.getControllerValue())
                        .install());

    }

}
