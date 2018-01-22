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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Generic operation handler for an executable management object.
 * @author Paul Ferraro
 * @param C the execution context
 * @param E the contextual executable
 */
public class ExecutionHandler<C, E extends Executable<C>> extends AbstractRuntimeOnlyHandler {

    private final Map<String, E> executables = new HashMap<>();
    private final Executor<C, E> executor;
    private final Function<ModelNode, String> nameExtractor;

    /**
     * Constructs a new ExecutionHandler
     * @param executor an executor
     * @param executables the executables sharing this handler
     * @param name a function returning the name of an executable
     */
    public ExecutionHandler(Executor<C, E> executor, Collection<? extends E> executables, Function<E, String> nameFactory, Function<ModelNode, String> nameExtractor) {
        this.executor = executor;
        for (E executable : executables) {
            this.executables.put(nameFactory.apply(executable), executable);
        }
        this.nameExtractor = nameExtractor;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        String name = this.nameExtractor.apply(operation);
        E executable = this.executables.get(name);
        try {
            ModelNode result = this.executor.execute(context, executable);
            if (result != null) {
                context.getResult().set(result);
            }
        } catch (OperationFailedException e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }
}
