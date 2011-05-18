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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation responsible for changing the file attributes of file based logging handlers.
 *
 * @author John Bailey
 */
public class HandlerFileChange implements ModelUpdateOperationHandler {
    static final String OPERATION_NAME = "change-file";
    static final HandlerFileChange INSTANCE = new HandlerFileChange();

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final ModelNode opAddr = operation.require(OP_ADDR);

        final ModelNode model = context.getSubModel();

        final ModelNode existingFile = model.get(CommonAttributes.FILE);

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(OPERATION_NAME);
        compensatingOperation.get(OP_ADDR).set(opAddr);

        compensatingOperation.get(CommonAttributes.PATH).set(existingFile.get(CommonAttributes.PATH));
        existingFile.get(CommonAttributes.PATH).set(operation.get(CommonAttributes.PATH));

        if (existingFile.hasDefined(CommonAttributes.RELATIVE_TO)) {
            compensatingOperation.get(CommonAttributes.RELATIVE_TO).set(existingFile.get(CommonAttributes.RELATIVE_TO));
            existingFile.get(CommonAttributes.RELATIVE_TO).set(operation.get(CommonAttributes.RELATIVE_TO));
        }

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry();
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    final ServiceController<?> controller = serviceRegistry.getService(LogServices.handlerFileName(name));
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {
                            public void listenerAdded(ServiceController<?> controller) {
                                controller.setMode(ServiceController.Mode.REMOVE);
                            }

                            public void serviceRemoved(ServiceController<?> controller) {
                                installService(operation, serviceTarget, name, resultHandler);
                            }
                        });
                    } else {
                        installService(operation, serviceTarget, name, resultHandler);
                    }

                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    private void installService(ModelNode operation, ServiceTarget serviceTarget, String name, final ResultHandler resultHandler) {
        final HandlerFileService service = new HandlerFileService(operation.get(CommonAttributes.PATH).asString());
        final ServiceBuilder<?> builder = serviceTarget.addService(LogServices.handlerFileName(name), service);
        if (operation.hasDefined(CommonAttributes.RELATIVE_TO)) {
            builder.addDependency(AbstractPathService.pathNameOf(operation.get(RELATIVE_TO).asString()), String.class, service.getRelativeToInjector());
        }
        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new AbstractServiceListener<Object>() {
                    public void serviceStarted(ServiceController<?> controller) {
                        resultHandler.handleResultComplete();
                    }
                }).install();
    }
}
