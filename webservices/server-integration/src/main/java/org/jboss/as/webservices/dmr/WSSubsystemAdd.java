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
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.as.webservices.util.ModuleClassLoaderProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class WSSubsystemAdd extends AbstractBoottimeAddStepHandler {
    static final WSSubsystemAdd INSTANCE = new WSSubsystemAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : Attributes.SUBSYSTEM_ATTRIBUTES) {
            attr.validateAndSet(operation, model);
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
            ServerConfigImpl serverConfig = createServerConfig(model, true, context);
            newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler, getServerConfigDependencies(context, appclient)));
        }
        if (!appclient) {
            ServerConfigImpl serverConfig = createServerConfig(model, false, context);
            newControllers.add(ServerConfigService.install(serviceTarget, serverConfig, verificationHandler, getServerConfigDependencies(context, appclient)));
        }
    }

    private static ServerConfigImpl createServerConfig(ModelNode configuration, boolean appclient, OperationContext context) throws OperationFailedException {
        final ServerConfigImpl config = ServerConfigImpl.newInstance();
        try {
            ModelNode wsdlHost = Attributes.WSDL_HOST.resolveModelAttribute(context, configuration);
            config.setWebServiceHost(wsdlHost.isDefined() ? wsdlHost.asString() : null);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        if (!appclient) {
            config.setModifySOAPAddress(Attributes.MODIFY_WSDL_ADDRESS.resolveModelAttribute(context, configuration).asBoolean());
        }
        if (configuration.hasDefined(WSDL_PORT)) {
            config.setWebServicePort(Attributes.WSDL_PORT.resolveModelAttribute(context, configuration).asInt());
        }
        if (configuration.hasDefined(WSDL_SECURE_PORT)) {
            config.setWebServiceSecurePort(Attributes.WSDL_SECURE_PORT.resolveModelAttribute(context, configuration).asInt());
        }
        return config;
    }

    /**
     * Process the model to figure out the name of the services the server config service has to depend on
     *
     */
    private static List<ServiceName> getServerConfigDependencies(OperationContext context, boolean appclient) {
        final List<ServiceName> serviceNames = new ArrayList<ServiceName>();
        final Resource subsystemResource = context.readResourceFromRoot(PathAddress.pathAddress(WSExtension.SUBSYSTEM_PATH));
        readConfigServiceNames(serviceNames, subsystemResource, Constants.CLIENT_CONFIG);
        readConfigServiceNames(serviceNames, subsystemResource, Constants.ENDPOINT_CONFIG);
        if (!appclient) {
            serviceNames.add(CommonWebServer.SERVICE_NAME);
        }
        return serviceNames;
    }

    private static void readConfigServiceNames(List<ServiceName> serviceNames, Resource subsystemResource, String configType) {
        for (ResourceEntry re : subsystemResource.getChildren(configType)) {
            ServiceName configServiceName = Constants.CLIENT_CONFIG.equals(configType) ? PackageUtils
                    .getClientConfigServiceName(re.getName()) : PackageUtils.getEndpointConfigServiceName(re.getName());
            serviceNames.add(configServiceName);
            readHandlerChainServiceNames(serviceNames, re, Constants.PRE_HANDLER_CHAIN, configServiceName);
            readHandlerChainServiceNames(serviceNames, re, Constants.POST_HANDLER_CHAIN, configServiceName);
            for (String propertyName : re.getChildrenNames(Constants.PROPERTY)) {
                serviceNames.add(PackageUtils.getPropertyServiceName(configServiceName, propertyName));
            }
        }
    }

    private static void readHandlerChainServiceNames(List<ServiceName> serviceNames, Resource configResource, String chainType, ServiceName configServiceName) {
        for (ResourceEntry re : configResource.getChildren(chainType)) {
            ServiceName handlerChainServiceName = PackageUtils.getHandlerChainServiceName(configServiceName, re.getName());
            serviceNames.add(handlerChainServiceName);
            for (String handlerName : re.getChildrenNames(Constants.HANDLER)) {
                serviceNames.add(PackageUtils.getHandlerServiceName(handlerChainServiceName, handlerName));
            }
        }
    }

}
