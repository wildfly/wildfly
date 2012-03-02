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

package org.jboss.as.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.web.Constants.EXECUTOR;
import static org.jboss.as.web.Constants.MAX_CONNECTIONS;
import static org.jboss.as.web.Constants.MAX_POST_SIZE;
import static org.jboss.as.web.Constants.MAX_SAVE_POST_SIZE;
import static org.jboss.as.web.Constants.PROXY_NAME;
import static org.jboss.as.web.Constants.PROXY_PORT;
import static org.jboss.as.web.Constants.REDIRECT_PORT;
import static org.jboss.as.web.Constants.VIRTUAL_SERVER;
import static org.jboss.as.web.WebConnectorDefinition.CONNECTOR_ATTRIBUTES;
import static org.jboss.as.web.WebExtension.SSL_PATH;

import java.util.List;
import java.util.concurrent.Executor;

import org.apache.catalina.connector.Connector;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * {@code OperationHandler} responsible for adding a web connector.
 *
 * @author Emanuel Muckenhuber
 */
class WebConnectorAdd extends AbstractAddStepHandler {


    static final WebConnectorAdd INSTANCE = new WebConnectorAdd();

    private WebConnectorAdd() {
        //
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        model.get(WebConnectorDefinition.NAME.getName()).set(address.getLastElement().getValue());

        for (SimpleAttributeDefinition def : CONNECTOR_ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final String bindingRef = WebConnectorDefinition.SOCKET_BINDING.resolveModelAttribute(context, fullModel).asString();

        final boolean enabled = WebConnectorDefinition.ENABLED.resolveModelAttribute(context, fullModel).asBoolean();
        final String protocol = WebConnectorDefinition.PROTOCOL.resolveModelAttribute(context, fullModel).asString();
        final String scheme = WebConnectorDefinition.SCHEME.resolveModelAttribute(context, fullModel).asString();
        final WebConnectorService service = new WebConnectorService(protocol, scheme);
        service.setSecure(WebConnectorDefinition.SECURE.resolveModelAttribute(context, fullModel).asBoolean());
        service.setEnableLookups(WebConnectorDefinition.ENABLE_LOOKUPS.resolveModelAttribute(context, fullModel).asBoolean());
        if (operation.hasDefined(PROXY_NAME)) {
            service.setProxyName(WebConnectorDefinition.PROXY_NAME.resolveModelAttribute(context, fullModel).asString());
        }
        if (operation.hasDefined(PROXY_PORT)) {
            service.setProxyPort(WebConnectorDefinition.PROXY_PORT.resolveModelAttribute(context, fullModel).asInt());
        }
        if (operation.hasDefined(REDIRECT_PORT)) {
            service.setRedirectPort(WebConnectorDefinition.REDIRECT_PORT.resolveModelAttribute(context, fullModel).asInt());
        }
        if (operation.hasDefined(MAX_POST_SIZE)) {
            service.setMaxPostSize(WebConnectorDefinition.MAX_POST_SIZE.resolveModelAttribute(context, fullModel).asInt());
        }
        if (operation.hasDefined(MAX_SAVE_POST_SIZE)) {
            service.setMaxSavePostSize(WebConnectorDefinition.MAX_SAVE_POST_SIZE.resolveModelAttribute(context, fullModel).asInt());
        }
        if (operation.hasDefined(MAX_CONNECTIONS)) {
            service.setMaxConnections(WebConnectorDefinition.MAX_CONNECTIONS.resolveModelAttribute(context, fullModel).asInt());
        }
        if (operation.hasDefined(VIRTUAL_SERVER)) {
            service.setVirtualServers(operation.get(VIRTUAL_SERVER).clone());
        }
        if (fullModel.get(SSL_PATH.getKey(), SSL_PATH.getValue()).isDefined()) {
            service.setSsl(resolveExpressions(context, fullModel.get(SSL_PATH.getKey(), SSL_PATH.getValue())));
        }
        final ServiceBuilder<Connector> serviceBuilder = context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB_CONNECTOR.append(name), service)
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getServer())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding());
        if (operation.hasDefined(EXECUTOR)) {
            String executorRef = operation.get(EXECUTOR).asString();
            serviceBuilder.addDependency(ThreadsServices.executorName(executorRef), Executor.class, service.getExecutor());
        }
        serviceBuilder.setInitialMode(enabled ? Mode.ACTIVE : Mode.NEVER);
        if (enabled) {
            serviceBuilder.addListener(verificationHandler);
        }
        final ServiceController<Connector> serviceController = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }
    }

    private ModelNode resolveExpressions(OperationContext context, ModelNode ssl) throws OperationFailedException {
        ModelNode result = new ModelNode();
        for (AttributeDefinition def : WebSSLDefinition.SSL_ATTRIBUTES) {
            result.get(def.getName()).set(def.resolveModelAttribute(context, ssl));
        }
        return result;
    }

}
