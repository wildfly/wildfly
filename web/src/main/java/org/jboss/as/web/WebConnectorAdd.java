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

import org.apache.catalina.connector.Connector;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.server.RuntimeTask;
import org.jboss.as.server.RuntimeTaskContext;
import static org.jboss.as.web.CommonAttributes.ENABLED;
import static org.jboss.as.web.CommonAttributes.ENABLE_LOOKUPS;
import static org.jboss.as.web.CommonAttributes.EXECUTOR;
import static org.jboss.as.web.CommonAttributes.MAX_POST_SIZE;
import static org.jboss.as.web.CommonAttributes.MAX_SAVE_POST_SIZE;
import static org.jboss.as.web.CommonAttributes.PROTOCOL;
import static org.jboss.as.web.CommonAttributes.PROXY_NAME;
import static org.jboss.as.web.CommonAttributes.PROXY_PORT;
import static org.jboss.as.web.CommonAttributes.REDIRECT_PORT;
import static org.jboss.as.web.CommonAttributes.SCHEME;
import static org.jboss.as.web.CommonAttributes.SECURE;
import static org.jboss.as.web.CommonAttributes.SOCKET_BINDING;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * {@code OperationHandler} responsible for adding a web connector.
 *
 * @author Emanuel Muckenhuber
 */
class WebConnectorAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

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
        if (existing.hasDefined(MAX_SAVE_POST_SIZE)) op.get(MAX_SAVE_POST_SIZE).set(existing.get(MAX_SAVE_POST_SIZE).asInt());

        return op;
    }

    static final WebConnectorAdd INSTANCE = new WebConnectorAdd();

    private WebConnectorAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(OperationContext context, final ModelNode operation, ResultHandler resultHandler) {

        ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();
        final String bindingRef = operation.require(SOCKET_BINDING).asString();

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);

        final ModelNode subModel = context.getSubModel();
        subModel.get(PROTOCOL).set(operation.get(PROTOCOL));
        subModel.get(SOCKET_BINDING).set(operation.get(SOCKET_BINDING));
        if(operation.hasDefined(SCHEME)) subModel.get(SCHEME).set(operation.get(SCHEME));
        if(operation.hasDefined(SECURE)) subModel.get(SECURE).set(operation.get(SECURE).asBoolean());
        if(operation.hasDefined(ENABLED)) subModel.get(ENABLED).set(operation.get(ENABLED).asBoolean());
        if(operation.hasDefined(ENABLE_LOOKUPS)) subModel.get(ENABLE_LOOKUPS).set(operation.get(ENABLE_LOOKUPS).asBoolean());
        if(operation.hasDefined(EXECUTOR)) subModel.get(EXECUTOR).set(operation.get(EXECUTOR).asString());
        if(operation.hasDefined(PROXY_NAME)) subModel.get(PROXY_NAME).set(operation.get(PROXY_NAME).asString());
        if(operation.hasDefined(PROXY_PORT)) subModel.get(PROXY_PORT).set(operation.get(PROXY_PORT).asInt());
        if(operation.hasDefined(REDIRECT_PORT)) subModel.get(REDIRECT_PORT).set(operation.get(REDIRECT_PORT).asInt());
        if(operation.hasDefined(MAX_POST_SIZE)) subModel.get(MAX_POST_SIZE).set(operation.get(MAX_POST_SIZE).asInt());
        if(operation.hasDefined(MAX_SAVE_POST_SIZE)) subModel.get(MAX_SAVE_POST_SIZE).set(operation.get(MAX_SAVE_POST_SIZE).asInt());

        if (context instanceof RuntimeOperationContext) {
            RuntimeOperationContext.class.cast(context).executeRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context, final ResultHandler resultHandler) throws OperationFailedException {
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
                    final ServiceBuilder<Connector> serviceBuilder = context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB_CONNECTOR.append(name), service)
                            .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getServer())
                            .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding())
                            .setInitialMode(enabled ? Mode.ACTIVE : Mode.NEVER);
                    if(enabled) {
                        serviceBuilder.addListener(new ResultHandler.ServiceStartListener(resultHandler));
                        serviceBuilder.install();
                    } else {
                        serviceBuilder.install();
                        resultHandler.handleResultComplete();
                    }
                }
            }, resultHandler);
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

}
