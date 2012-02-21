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
package org.jboss.as.webservices.dmr;

import org.jboss.as.controller.PathAddress;
import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.net.UnknownHostException;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
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
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WSSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final WSSubsystemAdd INSTANCE = new WSSubsystemAdd();

    private final ParametersValidator configValidator = new ParametersValidator();

    // Private to ensure a singleton.
    private WSSubsystemAdd() {
        configValidator.registerValidator(WSDL_HOST, new ModelTypeValidator(ModelType.STRING, true, true));
        configValidator.registerValidator(MODIFY_WSDL_ADDRESS, new ModelTypeValidator(ModelType.BOOLEAN));
        configValidator.registerValidator(WSDL_PORT, new ModelTypeValidator(ModelType.INT, true, true));
        configValidator.registerValidator(WSDL_SECURE_PORT, new ModelTypeValidator(ModelType.INT, true, true));
    }

    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {

        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;
        final ModelNode submodel = resource.getModel();

        if (operation.hasDefined(WSDL_PORT)) {
            submodel.get(WSDL_PORT).set(operation.require(WSDL_PORT));
        }
        if (operation.hasDefined(WSDL_SECURE_PORT)) {
            submodel.get(WSDL_SECURE_PORT).set(operation.require(WSDL_SECURE_PORT));
        }

        if (appclient && operation.hasDefined(WSDL_HOST)) {
            submodel.get(WSDL_HOST).setExpression(operation.require(WSDL_HOST).asString());
        }

        if (!appclient) {
            submodel.get(WSDL_HOST).setExpression(operation.require(WSDL_HOST).asString());
            configValidator.validate(operation);
            submodel.get(MODIFY_WSDL_ADDRESS).set(operation.require(MODIFY_WSDL_ADDRESS));
            submodel.get(ENDPOINT_CONFIG).setEmptyObject();
            submodel.get(ENDPOINT).setEmptyObject();
        }
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        //NOOP this is not actually used
    }


    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        ROOT_LOGGER.activatingWebservicesExtension();
        ModuleClassLoaderProvider.register();

        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;


        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                // add the DUP for dealing with WS deployments
                WSDeploymentActivator.activate(processorTarget, appclient);
            }
        }, OperationContext.Stage.RUNTIME);

        WSServices.saveContainerRegistry(context.getServiceRegistry(false));
        ServiceTarget serviceTarget = context.getServiceTarget();
        if (appclient && model.hasDefined(WSDL_HOST)) {
            ServerConfigImpl serverConfig = createServerConfig(model, true);
            newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler));
        }
        if (!appclient) {
            ServerConfigImpl serverConfig = createServerConfig(model, false);
            newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler));
            newControllers.add(EndpointRegistryService.install(serviceTarget, verificationHandler));

            final Resource webSubsystem = context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement("subsystem", "web")));
            String defaultHost = webSubsystem.getModel().get("default-virtual-server").asString();
            newControllers.add(PortComponentLinkService.install(serviceTarget, defaultHost, verificationHandler));
        }
    }

    private static ServerConfigImpl createServerConfig(ModelNode configuration, boolean appclient) {
        final ServerConfigImpl config = ServerConfigImpl.newInstance();
        try {
            ModelNode wsdlHost = configuration.require(WSDL_HOST).resolve();
            config.setWebServiceHost(wsdlHost.asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        if (!appclient) {
            config.setModifySOAPAddress(configuration.require(MODIFY_WSDL_ADDRESS).asBoolean());
        }
        if (configuration.hasDefined(WSDL_PORT)) {
            config.setWebServicePort(configuration.require(WSDL_PORT).asInt());
        }
        if (configuration.hasDefined(WSDL_SECURE_PORT)) {
            config.setWebServiceSecurePort(configuration.require(WSDL_SECURE_PORT).asInt());
        }
        return config;
    }

}
