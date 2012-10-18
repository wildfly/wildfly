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

import java.util.LinkedList;
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
 * {@code OperationHandler} responsible for adding a web valve.
 *
 * @author Jean-Frederic Clere
 */
class WebValveAdd extends AbstractAddStepHandler {


    static final WebValveAdd INSTANCE = new WebValveAdd();

    private WebValveAdd() {
        //
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        model.get(WebValveDefinition.CLASS_NAME.getName()).set(address.getLastElement().getValue());

        for (SimpleAttributeDefinition def : WebValveDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
        WebValveDefinition.PARAMS.validateAndSet(operation,model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        // Need more...
    }
}
