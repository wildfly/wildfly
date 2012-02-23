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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.web.Constants.ENABLED;
import static org.jboss.as.web.Constants.ENABLE_LOOKUPS;
import static org.jboss.as.web.Constants.EXECUTOR;
import static org.jboss.as.web.Constants.MAX_CONNECTIONS;
import static org.jboss.as.web.Constants.MAX_POST_SIZE;
import static org.jboss.as.web.Constants.MAX_SAVE_POST_SIZE;
import static org.jboss.as.web.Constants.PROTOCOL;
import static org.jboss.as.web.Constants.PROXY_NAME;
import static org.jboss.as.web.Constants.PROXY_PORT;
import static org.jboss.as.web.Constants.REDIRECT_PORT;
import static org.jboss.as.web.Constants.SCHEME;
import static org.jboss.as.web.Constants.SECURE;
import static org.jboss.as.web.Constants.SOCKET_BINDING;
import static org.jboss.as.web.Constants.SSL;
import static org.jboss.as.web.Constants.VIRTUAL_SERVER;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import org.apache.catalina.connector.Connector;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * {@code OperationHandler} responsible for adding a web connector.
 *
 * @author Emanuel Muckenhuber
 */
class WebConnectorAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final String OPERATION_NAME = ADD;

    static ModelNode getRecreateOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(OPERATION_NAME, address);
        op.get(PROTOCOL).set(existing.get(PROTOCOL));
        op.get(SOCKET_BINDING).set(existing.get(SOCKET_BINDING));
        if (existing.hasDefined(SCHEME)) op.get(SCHEME).set(existing.get(SCHEME).asString());
        if (existing.hasDefined(SECURE)) op.get(SECURE).set(existing.get(SECURE).asBoolean());
        if (existing.hasDefined(ENABLED)) op.get(ENABLED).set(existing.get(ENABLED).asBoolean());
        if (existing.hasDefined(ENABLE_LOOKUPS)) op.get(ENABLE_LOOKUPS).set(existing.get(ENABLE_LOOKUPS).asBoolean());
        if (existing.hasDefined(EXECUTOR)) op.get(EXECUTOR).set(existing.get(EXECUTOR).asString());
        if (existing.hasDefined(PROXY_NAME)) op.get(PROXY_NAME).set(existing.get(PROXY_NAME).asString());
        if (existing.hasDefined(PROXY_PORT)) op.get(PROXY_PORT).set(existing.get(PROXY_PORT).asInt());
        if (existing.hasDefined(REDIRECT_PORT)) op.get(REDIRECT_PORT).set(existing.get(REDIRECT_PORT).asInt());
        if (existing.hasDefined(MAX_POST_SIZE)) op.get(MAX_POST_SIZE).set(existing.get(MAX_POST_SIZE).asInt());
        if (existing.hasDefined(MAX_SAVE_POST_SIZE))
            op.get(MAX_SAVE_POST_SIZE).set(existing.get(MAX_SAVE_POST_SIZE).asInt());
        if (existing.hasDefined(MAX_CONNECTIONS))
            op.get(Constants.MAX_CONNECTIONS).set(existing.get(Constants.MAX_CONNECTIONS).asInt());
        op.get(Constants.VIRTUAL_SERVER).set(existing.get(Constants.VIRTUAL_SERVER));
        op.get(Constants.SSL).set(existing.get(Constants.SSL));

        return op;
    }

    static final WebConnectorAdd INSTANCE = new WebConnectorAdd();

    private WebConnectorAdd() {
        //
    }

    @Override
    protected void populateModel(final ModelNode operation, final Resource resource) {
        final ModelNode model = resource.getModel();

        populateModel(operation, model);
        WebConfigurationHandlerUtils.initializeConnector(resource, operation);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode subModel) {
        subModel.get(PROTOCOL).set(operation.get(PROTOCOL));
        subModel.get(SOCKET_BINDING).set(operation.get(SOCKET_BINDING));
        if (operation.hasDefined(SCHEME)) subModel.get(SCHEME).set(operation.get(SCHEME));
        if (operation.hasDefined(SECURE)) subModel.get(SECURE).set(operation.get(SECURE).asBoolean());
        if (operation.hasDefined(ENABLED)) subModel.get(ENABLED).set(operation.get(ENABLED).asBoolean());
        if (operation.hasDefined(ENABLE_LOOKUPS))
            subModel.get(ENABLE_LOOKUPS).set(operation.get(ENABLE_LOOKUPS).asBoolean());
        if (operation.hasDefined(EXECUTOR)) subModel.get(EXECUTOR).set(operation.get(EXECUTOR).asString());
        if (operation.hasDefined(PROXY_NAME)) subModel.get(PROXY_NAME).set(operation.get(PROXY_NAME).asString());
        if (operation.hasDefined(PROXY_PORT)) subModel.get(PROXY_PORT).set(operation.get(PROXY_PORT).asInt());
        if (operation.hasDefined(REDIRECT_PORT)) subModel.get(REDIRECT_PORT).set(operation.get(REDIRECT_PORT).asInt());
        if (operation.hasDefined(MAX_POST_SIZE)) subModel.get(MAX_POST_SIZE).set(operation.get(MAX_POST_SIZE).asInt());
        if (operation.hasDefined(MAX_SAVE_POST_SIZE))
            subModel.get(MAX_SAVE_POST_SIZE).set(operation.get(MAX_SAVE_POST_SIZE).asInt());
        if (operation.hasDefined(MAX_CONNECTIONS))
            subModel.get(Constants.MAX_CONNECTIONS).set(operation.get(Constants.MAX_CONNECTIONS).asInt());
        subModel.get(Constants.VIRTUAL_SERVER).set(operation.get(Constants.VIRTUAL_SERVER));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String bindingRef = operation.require(SOCKET_BINDING).asString();

        final boolean enabled = operation.hasDefined(ENABLED) ? operation.get(ENABLED).asBoolean() : true;
        final WebConnectorService service = new WebConnectorService(operation.require(PROTOCOL).asString(), operation.get(SCHEME).asString());
        if (operation.hasDefined(SECURE)) service.setSecure(operation.get(SECURE).asBoolean());
        if (operation.hasDefined(ENABLE_LOOKUPS))
            service.setEnableLookups(operation.get(ENABLE_LOOKUPS).asBoolean());
        if (operation.hasDefined(PROXY_NAME)) service.setProxyName(operation.get(PROXY_NAME).asString());
        if (operation.hasDefined(PROXY_PORT)) service.setProxyPort(operation.get(PROXY_PORT).asInt());
        if (operation.hasDefined(REDIRECT_PORT))
            service.setRedirectPort(operation.get(REDIRECT_PORT).asInt());
        if (operation.hasDefined(MAX_POST_SIZE))
            service.setMaxPostSize(operation.get(MAX_POST_SIZE).asInt());
        if (operation.hasDefined(MAX_SAVE_POST_SIZE))
            service.setMaxSavePostSize(operation.get(MAX_SAVE_POST_SIZE).asInt());
        if (operation.hasDefined(MAX_CONNECTIONS))
            service.setMaxConnections(operation.get(MAX_CONNECTIONS).asInt());
        if (operation.hasDefined(VIRTUAL_SERVER))
            service.setVirtualServers(operation.get(VIRTUAL_SERVER).clone());
        if (operation.hasDefined(SSL)) {
            service.setSsl(resolveExpressions(context,operation.get(SSL)));
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
        if(newControllers != null) {
            newControllers.add(serviceController);
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return WebSubsystemDescriptions.getConnectorAdd(locale);
    }

    private ModelNode resolveExpressions(OperationContext context, ModelNode connector) throws OperationFailedException {
        ModelNode result = connector.clone();
        for (Property p :connector.asPropertyList()){
            ModelNode node = p.getValue();
            if (node.getType() == ModelType.EXPRESSION){
                result.get(p.getName()).set(context.resolveExpressions(node));
            }
        }
        return result;
    }

}
