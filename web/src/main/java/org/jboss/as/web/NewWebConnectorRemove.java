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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.web.CommonAttributes.ENABLE_LOOKUPS;
import static org.jboss.as.web.CommonAttributes.MAX_POST_SIZE;
import static org.jboss.as.web.CommonAttributes.MAX_SAVE_POST_SIZE;
import static org.jboss.as.web.CommonAttributes.PROXY_NAME;
import static org.jboss.as.web.CommonAttributes.PROXY_PORT;
import static org.jboss.as.web.CommonAttributes.REDIRECT_PORT;
import static org.jboss.as.web.CommonAttributes.SECURE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author Emanuel Muckenhuber
 */
public class NewWebConnectorRemove implements ModelRemoveOperationHandler, RuntimeOperationHandler {

    static final NewWebConnectorRemove INSTANCE = new NewWebConnectorRemove();

    private NewWebConnectorRemove() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();

        final ModelNode subModel = context.getSubModel();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(ADD);
        compensatingOperation.get(OP_ADDR).set(address);
        compensatingOperation.get(REQUEST_PROPERTIES, SECURE).set(subModel.get(SECURE).asBoolean());
        compensatingOperation.get(REQUEST_PROPERTIES, ENABLE_LOOKUPS).set(subModel.get(ENABLE_LOOKUPS).asBoolean());
        compensatingOperation.get(REQUEST_PROPERTIES, PROXY_NAME).set(subModel.get(PROXY_NAME).asString());
        compensatingOperation.get(REQUEST_PROPERTIES, PROXY_PORT).set(subModel.get(PROXY_PORT).asInt());
        compensatingOperation.get(REQUEST_PROPERTIES, REDIRECT_PORT).set(subModel.get(REDIRECT_PORT).asInt());
        compensatingOperation.get(REQUEST_PROPERTIES, MAX_POST_SIZE).set(subModel.get(MAX_POST_SIZE).asInt());
        compensatingOperation.get(REQUEST_PROPERTIES, MAX_SAVE_POST_SIZE).set(subModel.get(MAX_SAVE_POST_SIZE).asInt());

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> service = runtimeContext.getServiceRegistry().getService(WebSubsystemElement.JBOSS_WEB_HOST.append(name));
            if(service != null) {
                // FIXME
                service.setMode(Mode.REMOVE);
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}