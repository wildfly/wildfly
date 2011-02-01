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

import static org.jboss.as.webservices.CommonAttributes.MODIFY_SOAP_ADDRESS;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_HOST;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_PORT;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_SECURE_PORT;
import static org.jboss.as.webservices.CommonAttributes.CONFIGURATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

import javax.management.MBeanServer;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewBootOperationContext;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.webservices.config.ServerConfigImpl;
import org.jboss.as.webservices.deployers.AspectDeploymentProcessor;
import org.jboss.as.webservices.deployers.WSDependenciesProcessor;
import org.jboss.as.webservices.deployers.WSDescriptorDeploymentProcessor;
import org.jboss.as.webservices.deployers.WSModelDeploymentProcessor;
import org.jboss.as.webservices.deployers.WSTypeDeploymentProcessor;
import org.jboss.as.webservices.parser.WSDeploymentAspectParser;
import org.jboss.as.webservices.service.EndpointRegistryService;
import org.jboss.as.webservices.service.ServerConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.common.management.AbstractServerConfig;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NewWSSubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    private static final ServiceName mbeanServiceName = ServiceName.JBOSS.append("mbean", "server");

    static final NewWSSubsystemAdd INSTANCE = new NewWSSubsystemAdd();

    private NewWSSubsystemAdd() {
        // Private to ensure a singleton.
    }

    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        // Create the compensating operation
        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if (context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            ServiceTarget serviceTarget = updateContext.getServiceTarget();
            addConfigService(serviceTarget, createWSConfigurtionElement(operation));
            addRegistryService(serviceTarget);
        }

        if (context instanceof NewBootOperationContext) {
            final NewBootOperationContext operationContext = (NewBootOperationContext) context;
            int priority = Phase.INSTALL_WAR_METADATA + 10;

            operationContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEBSERVICES_XML,
                    new WSDescriptorDeploymentProcessor());
            // updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_JMS_WS_XML, new
            // WSJMSDescriptorDeploymentProcessor());
            operationContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WS, new WSDependenciesProcessor());
            // updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXRPC, new
            // WSJAXRPCDependenciesDeploymentProcessor());
            // updateContext.addDeploymentProcessor(Phase.INSTALL, priority++, new WSEJBAdapterDeploymentProcessor());
            operationContext.addDeploymentProcessor(Phase.INSTALL, priority++, new WSTypeDeploymentProcessor());
            operationContext.addDeploymentProcessor(Phase.INSTALL, priority++, new WSModelDeploymentProcessor());

            addDeploymentProcessors(NewWSSubsystemAdd.class.getClassLoader(), operationContext, priority);
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(CONFIGURATION).set(operation.require(CONFIGURATION));
        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    // TODO - This needs to be reconsidered to decide if there should be a conversion from the
    // detyped API to the typed API - if this is only required in the integration layer it may
    // make sense to just use the detyped API.
    private WSConfigurationElement createWSConfigurtionElement(ModelNode operation) {
        WSConfigurationElement configurationElement = new WSConfigurationElement();

        ModelNode configuration = operation.require(CONFIGURATION);
        configurationElement.setWebServiceHost(configuration.require(WEBSERVICE_HOST).asString());
        configurationElement.setModifySOAPAddress(configuration.require(MODIFY_SOAP_ADDRESS).asBoolean());
        if (configuration.has(WEBSERVICE_PORT)) {
            configurationElement.setWebServicePort(configuration.require(WEBSERVICE_PORT).asInt());
        }
        if (configuration.has(WEBSERVICE_SECURE_PORT)) {
            configurationElement.setWebServiceSecurePort(configuration.require(WEBSERVICE_SECURE_PORT).asInt());
        }

        return configurationElement;
    }

    private void addDeploymentProcessors(final ClassLoader cl, final NewBootOperationContext operationContext, int priority) {
        try {
            Enumeration<URL> urls = cl.getResources("/META-INF/deployment-aspects.xml");
            if (urls != null) {
                ClassLoader origClassLoader = SecurityActions.getContextClassLoader();
                try {
                    SecurityActions.setContextClassLoader(cl);
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        InputStream is = null;
                        try {
                            is = url.openStream();
                            List<DeploymentAspect> deploymentAspects = WSDeploymentAspectParser.parse(is);
                            for (DeploymentAspect da : deploymentAspects) {
                                int p = priority + da.getRelativeOrder();
                                operationContext.addDeploymentProcessor(Phase.INSTALL, p, new AspectDeploymentProcessor(da));
                            }
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        }
                    }
                } finally {
                    SecurityActions.setContextClassLoader(origClassLoader);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load WS deployment aspects!", e);
        }
    }

    private static void addConfigService(ServiceTarget serviceTarget, WSConfigurationElement configuration) {
        InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        InjectedValue<ServerEnvironment> serverEnvironment = new InjectedValue<ServerEnvironment>();
        AbstractServerConfig serverConfig = createServerConfig(configuration, mbeanServer, serverEnvironment);
        serviceTarget.addService(WSServices.CONFIG_SERVICE, new ServerConfigService(serverConfig))
                .addDependency(mbeanServiceName, MBeanServer.class, mbeanServer)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, serverEnvironment)
                .setInitialMode(Mode.ACTIVE).install();
    }

    private static void addRegistryService(ServiceTarget serviceTarget) {
        serviceTarget.addService(WSServices.REGISTRY_SERVICE, new EndpointRegistryService()).setInitialMode(Mode.ACTIVE)
                .install();
    }

    private static AbstractServerConfig createServerConfig(WSConfigurationElement configuration,
            InjectedValue<MBeanServer> mbeanServer, InjectedValue<ServerEnvironment> serverEnvironment) {
        AbstractServerConfig config = new ServerConfigImpl(mbeanServer, serverEnvironment);
        try {
            config.setWebServiceHost(configuration.getWebServiceHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        config.setModifySOAPAddress(configuration.isModifySOAPAddress());
        if (configuration.getWebServicePort() != null) {
            config.setWebServicePort(configuration.getWebServicePort());
        }
        if (configuration.getWebServiceSecurePort() != null) {
            config.setWebServicePort(configuration.getWebServiceSecurePort());
        }
        return config;
    }

}
