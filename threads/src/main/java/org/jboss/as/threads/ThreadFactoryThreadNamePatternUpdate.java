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

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Brian Stansberry
 */
public final class ThreadFactoryThreadNamePatternUpdate implements NewStepHandler {

    private static final long serialVersionUID = 4253625376544201028L;

    public static final ThreadFactoryThreadNamePatternUpdate INSTANCE = new ThreadFactoryThreadNamePatternUpdate();

    private final ParametersValidator validator = new ParametersValidator();

    private ThreadFactoryThreadNamePatternUpdate() {
        validator.registerValidator(VALUE, new ModelTypeValidator(ModelType.STRING, true, true));
    }

    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        validator.validate(operation);

        final String name = Util.getNameFromAddress(operation.require(OP_ADDR));

        ModelNode model = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        if (!model.isDefined()) {
            throw new OperationFailedException(notConfigured(name));
        }

        ModelNode oldValue = model.get(CommonAttributes.THREAD_NAME_PATTERN);
        final String newNamePattern;
        ModelNode newValue;
        if (operation.hasDefined(VALUE)) {
            newValue = operation.get(VALUE);
            newNamePattern = newValue.resolve().asString(); // TODO validate resolved value
        } else {
            newValue = new ModelNode();
            newNamePattern = null;
        }

        model.get(CommonAttributes.THREAD_NAME_PATTERN).set(newValue);

        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> service = context.getServiceRegistry(false).getService(ThreadsServices.threadFactoryName(name));
                    if (service == null) {
                        throw new OperationFailedException(notConfigured(name));
                    } else {
                        final ThreadFactoryService threadFactoryService = (ThreadFactoryService) service.getValue();
                        threadFactoryService.setNamePattern(newNamePattern);
                    }
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    private ModelNode notConfigured(String name) {
        return new ModelNode().set(String.format("No thread factory named %s is configured", name));
    }
}
