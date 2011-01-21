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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Emanuel Muckenhuber
 */
class NewRootLoggerRemove implements ModelUpdateOperationHandler, RuntimeOperationHandler {

    static final NewRootLoggerRemove INSTANCE = new NewRootLoggerRemove();

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode subModel = context.getSubModel();
        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set("set-root-logger");
        compensatingOperation.get(CommonAttributes.LEVEL).set(subModel.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.LEVEL));
        compensatingOperation.get(CommonAttributes.HANDLERS).set(subModel.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.HANDLERS));

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceRegistry registry = runtimeContext.getServiceRegistry();
            final ServiceController<?> controller = runtimeContext.getServiceRegistry().getService(LogServices.ROOT_LOGGER);
            if(controller != null) {
                controller.setMode(ServiceController.Mode.REMOVE);
            }
            if(subModel.get(CommonAttributes.ROOT_LOGGER).has(CommonAttributes.HANDLERS)) {
                LogServices.uninstallLoggerHandlers(registry, "", subModel.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.HANDLERS));
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
