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

package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry
 */
public final class ThreadFactoryPriorityUpdate implements RuntimeOperationHandler, ModelUpdateOperationHandler {

    private static final long serialVersionUID = 4253625376544201028L;

    public static final ThreadFactoryPriorityUpdate INSTANCE = new ThreadFactoryPriorityUpdate();

    private final ParametersValidator validator = new ParametersValidator();
    private ThreadFactoryPriorityUpdate() {
        validator.registerValidator(VALUE, new IntRangeValidator(1, 10, true, true));
    }

    @Override
    public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        String failure = validator.validate(operation);
        if (failure != null) {
            resultHandler.handleFailed(new ModelNode().set(failure));
            return Cancellable.NULL;
        }

        final String name = Util.getNameFromAddress(operation.require(OP_ADDR));

        ModelNode model = context.getSubModel();
        if (!model.isDefined()) {
            resultHandler.handleFailed(notConfigured(name));
            return Cancellable.NULL;
        }

        ModelNode oldValue = model.get(CommonAttributes.PRIORITY);
        Integer newPriority = null;
        ModelNode newValue;
        if (operation.hasDefined(VALUE)) {
            newValue = operation.get(VALUE);
            newPriority = Integer.valueOf(newValue.resolve().asInt()); // TODO validate resolved value
        }
        else {
            newValue = new ModelNode();
        }

        model.get(CommonAttributes.PRIORITY).set(newValue);

        if (context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext updateContext = (RuntimeOperationContext) context;
            final ServiceController<?> service = updateContext.getServiceRegistry().getService(ThreadsServices.threadFactoryName(name));
            if (service == null) {
                resultHandler.handleFailed(notConfigured(name));
                return Cancellable.NULL;
            } else {
                final ThreadFactoryService threadFactoryService = (ThreadFactoryService) service.getValue();
                threadFactoryService.setPriority(newPriority);
            }
        }

        final ModelNode compensatingOp = operation.clone();
        compensatingOp.get(VALUE).set(oldValue);

        resultHandler.handleResultComplete(compensatingOp);

        return Cancellable.NULL;
    }

    private ModelNode notConfigured(String name) {
        return new ModelNode().set(String.format("No thread factory named %s is configured", name));
    }
}
