/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Generic {@link org.jboss.as.controller.OperationStepHandler} for runtime operations.
 * @author Paul Ferraro
 */
public class OperationHandler<C> extends AbstractRuntimeOnlyHandler implements ManagementRegistrar<ManagementResourceRegistration> {

    private final Collection<? extends Operation<C>> operations;
    private final Map<String, Operation<C>> executables = new HashMap<>();
    private final OperationExecutor<C> executor;

    public <O extends Enum<O> & Operation<C>> OperationHandler(OperationExecutor<C> executor, Class<O> operationClass) {
        this(executor, EnumSet.allOf(operationClass));
    }

    public OperationHandler(OperationExecutor<C> executor, Operation<C>[] operations) {
        this(executor, Arrays.asList(operations));
    }

    public OperationHandler(OperationExecutor<C> executor, Collection<? extends Operation<C>> operations) {
        this.executor = executor;
        for (Operation<C> executable : operations) {
            this.executables.put(executable.getName(), executable);
        }
        this.operations = operations;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (Operation<C> operation : this.operations) {
            registration.registerOperationHandler(operation.getDefinition(), this);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        String name = operation.get(ModelDescriptionConstants.OP).asString();
        Operation<C> executable = this.executables.get(name);
        try {
            ModelNode result = this.executor.execute(context, operation, executable);
            if (result != null) {
                context.getResult().set(result);
            }
        } catch (OperationFailedException e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }
}
