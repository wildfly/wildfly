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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry
 */
public final class ThreadFactoryPriorityUpdate implements ModelUpdateOperationHandler {

    private static final long serialVersionUID = 4253625376544201028L;

    public static final ThreadFactoryPriorityUpdate INSTANCE = new ThreadFactoryPriorityUpdate();

    private final ParametersValidator validator = new ParametersValidator();
    private ThreadFactoryPriorityUpdate() {
        validator.registerValidator(VALUE, new IntRangeValidator(1, 10, true, true));
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        String failure = validator.validate(operation);
        if (failure != null) {
            throw new OperationFailedException(new ModelNode().set(failure));
        }

        final String name = Util.getNameFromAddress(operation.require(OP_ADDR));

        ModelNode model = context.getSubModel();
        if (!model.isDefined()) {
            throw new OperationFailedException(notConfigured(name));
        }

        ModelNode oldValue = model.get(CommonAttributes.PRIORITY);
        final Integer newPriority;
        ModelNode newValue;
        if (operation.hasDefined(VALUE)) {
            newValue = operation.get(VALUE);
            newPriority = Integer.valueOf(newValue.resolve().asInt()); // TODO validate resolved value
        }
        else {
            newValue = new ModelNode();
            newPriority = null;
        }

        model.get(CommonAttributes.PRIORITY).set(newValue);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceController<?> service = context.getServiceRegistry()
                            .getService(ThreadsServices.threadFactoryName(name));
                    if (service == null) {
                        throw new OperationFailedException(notConfigured(name));
                    } else {
                        final ThreadFactoryService threadFactoryService = (ThreadFactoryService) service.getValue();
                        threadFactoryService.setPriority(newPriority);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        final ModelNode compensatingOp = operation.clone();
        compensatingOp.get(VALUE).set(oldValue);
        return new BasicOperationResult(compensatingOp);
    }

    private ModelNode notConfigured(String name) {
        return new ModelNode().set(String.format("No thread factory named %s is configured", name));
    }
}
