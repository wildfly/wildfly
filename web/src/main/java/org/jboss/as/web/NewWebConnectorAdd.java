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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.web.CommonAttributes.*;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * {@code OperationHandler} responsible for adding a web connector.
 *
 * @author Emanuel Muckenhuber
 */
class NewWebConnectorAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewWebConnectorAdd INSTANCE = new NewWebConnectorAdd();

    private NewWebConnectorAdd() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();
        final String bindingRef = operation.get(REQUEST_PROPERTIES).require(SOCKET_BINDING).asString();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(address);

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;

            final boolean enabled = operation.get(REQUEST_PROPERTIES).has(ENABLED) ? operation.get(REQUEST_PROPERTIES, ENABLED).asBoolean() : true;
            final WebConnectorService service = new WebConnectorService(operation.get(REQUEST_PROPERTIES).require(PROTOCOL).asString(), operation.get(REQUEST_PROPERTIES, SCHEME).asString());
            if(operation.get(REQUEST_PROPERTIES).has(SECURE)) service.setSecure(operation.get(REQUEST_PROPERTIES, SECURE).asBoolean());
            if(operation.get(ENABLE_LOOKUPS).has(ENABLE_LOOKUPS)) service.setEnableLookups(operation.get(REQUEST_PROPERTIES, ENABLE_LOOKUPS).asBoolean());
            if(operation.get(REQUEST_PROPERTIES).has(PROXY_NAME)) service.setProxyName(operation.get(REQUEST_PROPERTIES, PROXY_NAME).asString());
            if(operation.get(REQUEST_PROPERTIES).has(PROXY_PORT)) service.setProxyPort(operation.get(REQUEST_PROPERTIES, PROXY_PORT).asInt());
            if(operation.get(REQUEST_PROPERTIES).has(REDIRECT_PORT)) service.setRedirectPort(operation.get(REQUEST_PROPERTIES, REDIRECT_PORT).asInt());
            if(operation.get(REQUEST_PROPERTIES).has(MAX_POST_SIZE)) service.setMaxPostSize(operation.get(REQUEST_PROPERTIES, MAX_POST_SIZE).asInt());
            if(operation.get(REQUEST_PROPERTIES).has(MAX_SAVE_POST_SIZE)) service.setMaxSavePostSize(operation.get(REQUEST_PROPERTIES, MAX_SAVE_POST_SIZE).asInt());
            runtimeContext.getServiceTarget().addService(WebSubsystemElement.JBOSS_WEB_CONNECTOR.append(name), service)
                .addDependency(WebSubsystemElement.JBOSS_WEB, WebServer.class, service.getServer())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getBinding())
                .setInitialMode(enabled ? Mode.ACTIVE : Mode.NEVER)
                .install();
        }

        final ModelNode subModel = context.getSubModel();
        if(operation.get(REQUEST_PROPERTIES).has(SECURE)) subModel.get(SECURE).set(operation.get(REQUEST_PROPERTIES, SECURE).asBoolean());
        if(operation.get(ENABLE_LOOKUPS).has(ENABLE_LOOKUPS)) subModel.get(ENABLE_LOOKUPS).set(operation.get(REQUEST_PROPERTIES, ENABLE_LOOKUPS).asBoolean());
        if(operation.get(REQUEST_PROPERTIES).has(PROXY_NAME)) subModel.get(PROXY_NAME).set(operation.get(REQUEST_PROPERTIES, PROXY_NAME).asString());
        if(operation.get(REQUEST_PROPERTIES).has(PROXY_PORT)) subModel.get(PROXY_PORT).set(operation.get(REQUEST_PROPERTIES, PROXY_PORT).asInt());
        if(operation.get(REQUEST_PROPERTIES).has(REDIRECT_PORT)) subModel.get(REDIRECT_PORT).set(operation.get(REQUEST_PROPERTIES, REDIRECT_PORT).asInt());
        if(operation.get(REQUEST_PROPERTIES).has(MAX_POST_SIZE)) subModel.get(MAX_POST_SIZE).set(operation.get(REQUEST_PROPERTIES, MAX_POST_SIZE).asInt());
        if(operation.get(REQUEST_PROPERTIES).has(MAX_SAVE_POST_SIZE)) subModel.get(MAX_SAVE_POST_SIZE).set(operation.get(REQUEST_PROPERTIES, MAX_SAVE_POST_SIZE).asInt());

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
