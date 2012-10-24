/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.dmr;

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.net.UnknownHostException;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.service.EndpointRegistryService;
import org.jboss.as.webservices.service.PortComponentLinkService;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.as.webservices.util.ModuleClassLoaderProvider;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WSSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final WSSubsystemAdd INSTANCE = new WSSubsystemAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr:Attributes.SUBSYSTEM_ATTRIBUTES){
            attr.validateAndSet(operation,model);
        }
    }


    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ROOT_LOGGER.activatingWebservicesExtension();
        ModuleClassLoaderProvider.register();

        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;


        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                // add the DUP for dealing with WS deployments
                WSDeploymentActivator.activate(processorTarget, appclient);
            }
        }, OperationContext.Stage.RUNTIME);

        ServiceTarget serviceTarget = context.getServiceTarget();
        if (appclient && model.hasDefined(WSDL_HOST)) {
            ServerConfigImpl serverConfig = createServerConfig(model, true,context);
            newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler));
        }
        if (!appclient) {
            ServerConfigImpl serverConfig = createServerConfig(model, false, context);
            newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler));
            newControllers.add(EndpointRegistryService.install(serviceTarget, verificationHandler));

            final Resource webSubsystem = context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement("subsystem", "web")));
            String defaultHost = webSubsystem.getModel().get("default-virtual-server").asString();
            newControllers.add(PortComponentLinkService.install(serviceTarget, defaultHost, verificationHandler));
        }
    }

    private static ServerConfigImpl createServerConfig(ModelNode configuration, boolean appclient, OperationContext context) throws OperationFailedException {
        final ServerConfigImpl config = ServerConfigImpl.newInstance();
        try {
            ModelNode wsdlHost = Attributes.WSDL_HOST.resolveModelAttribute(context, configuration);
            config.setWebServiceHost(wsdlHost.asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        if (!appclient) {
            config.setModifySOAPAddress(Attributes.MODIFY_WSDL_ADDRESS.resolveModelAttribute(context,configuration).asBoolean());
        }
        if (configuration.hasDefined(WSDL_PORT)) {
            config.setWebServicePort(Attributes.WSDL_PORT.resolveModelAttribute(context,configuration).asInt());
        }
        if (configuration.hasDefined(WSDL_SECURE_PORT)) {
            config.setWebServiceSecurePort(Attributes.WSDL_SECURE_PORT.resolveModelAttribute(context,configuration).asInt());
        }
        return config;
    }

}
