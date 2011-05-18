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

package org.jboss.as.logging;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation responsible for disabling a logging handler.
 *
 * @author John Bailey
 */
public class HandlerDisable implements ModelUpdateOperationHandler {
    static final HandlerDisable INSTANCE = new HandlerDisable();

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final ModelNode opAddr = operation.require(OP_ADDR);

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(ENABLE);
        compensatingOperation.get(OP_ADDR).set(opAddr);

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry();
                    final ServiceController<?> controller = serviceRegistry.getService(LogServices.handlerName(name));
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {
                            public void listenerAdded(ServiceController<?> serviceController) {
                                serviceController.setMode(ServiceController.Mode.NEVER);
                            }

                            public void serviceStopped(ServiceController<?> serviceController) {
                                resultHandler.handleResultComplete();
                            }
                        });
                    } else {
                        resultHandler.handleResultComplete();
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }
}
