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
class NewWebVirtualHostRemove implements ModelRemoveOperationHandler, RuntimeOperationHandler {

    static final NewWebVirtualHostRemove INSTANCE = new NewWebVirtualHostRemove();

    private NewWebVirtualHostRemove() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(address);

        final ModelNode subModel = context.getSubModel();

        compensatingOperation.get(REQUEST_PROPERTIES, CommonAttributes.ALIAS).set(subModel.get(CommonAttributes.ALIAS));
        compensatingOperation.get(REQUEST_PROPERTIES, CommonAttributes.ACCESS_LOG).set(subModel.get(CommonAttributes.ACCESS_LOG));
        compensatingOperation.get(REQUEST_PROPERTIES, CommonAttributes.REWRITE).set(subModel.get(CommonAttributes.REWRITE));

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> service = runtimeContext.getServiceRegistry().getService(WebSubsystemElement.JBOSS_WEB_HOST.append(name));
            if(service != null) {
                service.setMode(Mode.REMOVE);
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
