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

package org.jboss.as.logging.handlers.file;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;

import java.util.List;

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation responsible for changing the file attributes of file based logging handlers.
 *
 * @author John Bailey
 */
public class HandlerFileChange extends AbstractModelUpdateHandler {
    public static final String OPERATION_NAME = "change-file";
    public static final HandlerFileChange INSTANCE = new HandlerFileChange();

    @Override
    protected void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        PATH.validateAndSet(operation, model);
        RELATIVE_TO.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final HandlerFileService service = new HandlerFileService(PATH.resolveModelAttribute(context, model).asString());
        final ServiceBuilder<String> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);

        final ModelNode relativeTo = RELATIVE_TO.resolveModelAttribute(context, model);
        if (relativeTo.isDefined()) {
            serviceBuilder.addDependency(AbstractPathService.pathNameOf(relativeTo.asString()), String.class, service.getRelativeToInjector());
        }
        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

        if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
            context.removeService(LogServices.handlerFileName(name));
        }
    }
}
