/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Generic {@link org.jboss.as.controller.OperationStepHandler} for runtime operations.
 * @author Paul Ferraro
 */
public class OperationHandler<C> extends AbstractRuntimeOnlyHandler implements Registration<ManagementResourceRegistration> {

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
        String name = Operations.getName(operation);
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
