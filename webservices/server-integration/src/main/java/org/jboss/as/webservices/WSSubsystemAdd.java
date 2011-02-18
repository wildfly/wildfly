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
package org.jboss.as.webservices;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.CommonAttributes.CONFIGURATION;
import static org.jboss.as.webservices.CommonAttributes.MODIFY_SOAP_ADDRESS;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_HOST;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_PORT;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_SECURE_PORT;

import java.net.UnknownHostException;

import javax.management.MBeanServer;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.service.EndpointRegistryService;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.common.management.AbstractServerConfig;

/**
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @since 09-Nov-2010
 */
public class WSSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.webservices");

    private static final ServiceName mbeanServiceName = ServiceName.JBOSS.append("mbean", "server");

    static final WSSubsystemAdd INSTANCE = new WSSubsystemAdd();

    private final ParametersValidator operationValidator = new ParametersValidator();
    private final ParametersValidator configValidator = new ParametersValidator();

    // Private to ensure a singleton.
    private WSSubsystemAdd() {
        operationValidator.registerValidator(CONFIGURATION, new ModelTypeValidator(ModelType.OBJECT));
        configValidator.registerValidator(WEBSERVICE_HOST, new ModelTypeValidator(ModelType.STRING));
        configValidator.registerValidator(MODIFY_SOAP_ADDRESS, new ModelTypeValidator(ModelType.BOOLEAN));
        configValidator.registerValidator(WEBSERVICE_PORT, new ModelTypeValidator(ModelType.INT, true, true));
        configValidator.registerValidator(WEBSERVICE_PORT, new ModelTypeValidator(ModelType.INT, true, true));
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        operationValidator.validate(operation);
        final ModelNode config = operation.require(CONFIGURATION);
        configValidator.validate(config);

        final ModelNode subModel = context.getSubModel();
        subModel.get(CONFIGURATION).set(config);

        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    log.info("Activating WebServices Extension");
                    WSServices.saveContainerRegistry(context.getServiceRegistry());

                    ServiceTarget serviceTarget = context.getServiceTarget();
                    addConfigService(serviceTarget, config);
                    addRegistryService(serviceTarget);

                    //add the DUP for dealing with WS deployments
                    WSDeploymentActivator.activate(updateContext);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        // Create the compensating operation
        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));
        return new BasicOperationResult(compensatingOperation);
    }

    private static void addConfigService(ServiceTarget serviceTarget, ModelNode configuration) {
        InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        InjectedValue<ServerEnvironment> serverEnvironment = new InjectedValue<ServerEnvironment>();
        AbstractServerConfig serverConfig = createServerConfig(configuration, mbeanServer, serverEnvironment);
        serviceTarget.addService(WSServices.CONFIG_SERVICE, new ServerConfigService(serverConfig))
                .addDependency(mbeanServiceName, MBeanServer.class, mbeanServer)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, serverEnvironment)
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    private static void addRegistryService(ServiceTarget serviceTarget) {
        serviceTarget
                .addService(WSServices.REGISTRY_SERVICE, new EndpointRegistryService())
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    private static AbstractServerConfig createServerConfig(ModelNode configuration,
            InjectedValue<MBeanServer> mbeanServer, InjectedValue<ServerEnvironment> serverEnvironment) {
        AbstractServerConfig config = new ServerConfigImpl(mbeanServer, serverEnvironment);
        try {
            config.setWebServiceHost(configuration.require(WEBSERVICE_HOST).asString());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        config.setModifySOAPAddress(configuration.require(MODIFY_SOAP_ADDRESS).asBoolean());
        if (configuration.hasDefined(WEBSERVICE_PORT)) {
            config.setWebServicePort(configuration.require(WEBSERVICE_PORT).asInt());
        }
        if (configuration.hasDefined(WEBSERVICE_SECURE_PORT)) {
            config.setWebServicePort(configuration.require(WEBSERVICE_SECURE_PORT).asInt());
        }
        return config;
    }

}
