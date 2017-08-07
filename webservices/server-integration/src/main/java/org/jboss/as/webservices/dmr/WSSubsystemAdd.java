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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PATH_REWRITE_RULE;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_URI_SCHEME;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.JMXExtension;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.as.webservices.service.XTSClientIntegrationService;
import org.jboss.as.webservices.util.ModuleClassLoaderProvider;
import org.jboss.dmr.ModelNode;
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
        Attributes.STATISTICS_ENABLED.validateAndSet(operation, model);
        for (AttributeDefinition attr : Attributes.SUBSYSTEM_ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        WSLogger.ROOT_LOGGER.activatingWebservicesExtension();
        ModuleClassLoaderProvider.register();
        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                // add the DUP for dealing with WS deployments
                WSDeploymentActivator.activate(processorTarget, appclient);
            }
        }, OperationContext.Stage.RUNTIME);

        ServiceTarget serviceTarget = context.getServiceTarget();
        final boolean jmxAvailable = isJMXSubsystemAvailable(context);
        if (appclient && model.hasDefined(WSDL_HOST)) {
            ServerConfigImpl serverConfig = createServerConfig(model, true, context);
            ServerConfigService.install(serviceTarget, serverConfig, getServerConfigDependencies(context, appclient), jmxAvailable, false);
        }
        if (!appclient) {
            ServerConfigImpl serverConfig = createServerConfig(model, false, context);
            ServerConfigService.install(serviceTarget, serverConfig, getServerConfigDependencies(context, appclient), jmxAvailable, true);
        }
        XTSClientIntegrationService.install(serviceTarget);
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
            config.setStatisticsEnabled(Attributes.STATISTICS_ENABLED.resolveModelAttribute(context, configuration).asBoolean());
        }
        if (configuration.hasDefined(WSDL_PORT)) {
            config.setWebServicePort(Attributes.WSDL_PORT.resolveModelAttribute(context, configuration).asInt());
        }
        if (configuration.hasDefined(WSDL_SECURE_PORT)) {
            config.setWebServiceSecurePort(Attributes.WSDL_SECURE_PORT.resolveModelAttribute(context, configuration).asInt());
        }
        if (configuration.hasDefined(WSDL_URI_SCHEME)) {
            config.setWebServiceUriScheme(Attributes.WSDL_URI_SCHEME.resolveModelAttribute(context, configuration).asString());
        }
        if (configuration.hasDefined(WSDL_PATH_REWRITE_RULE)) {
            config.setWebServicePathRewriteRule(Attributes.WSDL_PATH_REWRITE_RULE.resolveModelAttribute(context, configuration).asString());
        }
        return config;
    }

    /**
     * Process the model to figure out the name of the services the server config service has to depend on
     *
     */
    private static List<ServiceName> getServerConfigDependencies(OperationContext context, boolean appclient) {
        final List<ServiceName> serviceNames = new ArrayList<ServiceName>();
        final Resource subsystemResource = context.readResourceFromRoot(PathAddress.pathAddress(WSExtension.SUBSYSTEM_PATH), false);
        readConfigServiceNames(serviceNames, subsystemResource, Constants.CLIENT_CONFIG);
        readConfigServiceNames(serviceNames, subsystemResource, Constants.ENDPOINT_CONFIG);
        if (!appclient) {
            serviceNames.add(CommonWebServer.SERVICE_NAME);
        }
        return serviceNames;
    }

    private static void readConfigServiceNames(List<ServiceName> serviceNames, Resource subsystemResource, String configType) {
        for (String name : subsystemResource.getChildrenNames(configType)) {
            ServiceName configServiceName = Constants.CLIENT_CONFIG.equals(configType) ? PackageUtils
                    .getClientConfigServiceName(name) : PackageUtils.getEndpointConfigServiceName(name);
            serviceNames.add(configServiceName);
        }
    }

    private static boolean isJMXSubsystemAvailable(final OperationContext context) {
        Resource root = context.readResourceFromRoot(PathAddress.pathAddress(PathAddress.EMPTY_ADDRESS), false);
        return root.hasChild(PathElement.pathElement(SUBSYSTEM, JMXExtension.SUBSYSTEM_NAME));
    }
}
