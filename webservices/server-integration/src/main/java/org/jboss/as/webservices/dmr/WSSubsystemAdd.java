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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.net.UnknownHostException;
import java.util.List;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.deployers.WebServiceRefAnnotationParsingProcessor;
import org.jboss.as.webservices.service.EndpointRegistryService;
import org.jboss.as.webservices.service.ModelUpdateService;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.as.webservices.util.ModuleClassLoaderProvider;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WSSubsystemAdd extends AbstractBoottimeAddStepHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.webservices");

    static final WSSubsystemAdd INSTANCE = new WSSubsystemAdd();

    private final ParametersValidator configValidator = new ParametersValidator();

    // Private to ensure a singleton.
    private WSSubsystemAdd() {
        configValidator.registerValidator(WSDL_HOST, new ModelTypeValidator(ModelType.STRING));
        configValidator.registerValidator(MODIFY_WSDL_ADDRESS, new ModelTypeValidator(ModelType.BOOLEAN));
        configValidator.registerValidator(WSDL_PORT, new ModelTypeValidator(ModelType.INT, true, true));
        configValidator.registerValidator(WSDL_SECURE_PORT, new ModelTypeValidator(ModelType.INT, true, true));
    }

    protected void populateModel(final ModelNode operation, final ModelNode submodel) throws OperationFailedException {
        configValidator.validate(operation);

        submodel.get(MODIFY_WSDL_ADDRESS).set(operation.require(MODIFY_WSDL_ADDRESS));
        submodel.get(WSDL_HOST).set(operation.require(WSDL_HOST));
        if (operation.has(WSDL_PORT)) {
            submodel.get(WSDL_PORT).set(operation.require(WSDL_PORT));
        }
        if (operation.has(WSDL_SECURE_PORT)) {
            submodel.get(WSDL_SECURE_PORT).set(operation.require(WSDL_SECURE_PORT));
        }
        submodel.get(ENDPOINT_CONFIG).setEmptyObject();
        submodel.get(ENDPOINT).setEmptyObject();
    }

    protected void performBoottime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                // add the DUP for dealing with WS deployments
                WSDeploymentActivator.activate(processorTarget);
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_SERVICE_INJECTION_ANNOTATION, new WebServiceRefAnnotationParsingProcessor());
            }
        }, NewOperationContext.Stage.RUNTIME);

        log.info("Activating WebServices Extension");
        ModuleClassLoaderProvider.register();
        WSServices.saveContainerRegistry(context.getServiceRegistry(false));

        ServiceTarget serviceTarget = context.getServiceTarget();

        ServerConfigImpl serverConfig = createServerConfig(operation);
        newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler));
        newControllers.add(ModelUpdateService.install(serviceTarget, verificationHandler));
        newControllers.add(EndpointRegistryService.install(serviceTarget, verificationHandler));
    }

    private static ServerConfigImpl createServerConfig(ModelNode configuration) {
        final ServerConfigImpl config = ServerConfigImpl.getInstance();
        try {
            config.setWebServiceHost(configuration.require(WSDL_HOST).asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        config.setModifySOAPAddress(configuration.require(MODIFY_WSDL_ADDRESS).asBoolean());
        if (configuration.hasDefined(WSDL_PORT)) {
            config.setWebServicePort(configuration.require(WSDL_PORT).asInt());
        }
        if (configuration.hasDefined(WSDL_SECURE_PORT)) {
            config.setWebServiceSecurePort(configuration.require(WSDL_SECURE_PORT).asInt());
        }
        return config;
    }

}
