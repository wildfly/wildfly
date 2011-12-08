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

package org.jboss.as.controller.operations.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ParsedBootOp;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * Special handler that executes extension initialization in parallel.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParallelExtensionAddHandler implements OperationStepHandler {

    private final ExecutorService executor;
    private final List<ParsedBootOp> extensionAdds = new ArrayList<ParsedBootOp>();

    public ParallelExtensionAddHandler(ExecutorService executorService) {
        this.executor = executorService;
    }

    public void addParsedOp(final ParsedBootOp op, final ExtensionAddHandler handler) {
        extensionAdds.add(new ParsedBootOp(op, handler));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        context.addStep(getParallelExtensionInitializeStep(), OperationContext.Stage.IMMEDIATE);

        for (int i = extensionAdds.size() -1; i >= 0; i--) { // Reverse order so they execute in normal order!
            ParsedBootOp op = extensionAdds.get(i);
            context.addStep(op.response, op.operation, op.handler, OperationContext.Stage.IMMEDIATE);
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    private OperationStepHandler getParallelExtensionInitializeStep() {

        return new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                long start = System.currentTimeMillis();
                final Map<String, Future<OperationFailedException>> futures = new LinkedHashMap<String, Future<OperationFailedException>>();
                for (ParsedBootOp op : extensionAdds) {
                    String module = op.address.getLastElement().getValue();
                    ExtensionAddHandler addHandler = ExtensionAddHandler.class.cast(op.handler);
                    Future<OperationFailedException> future = executor.submit(new ExtensionInitializeTask(module, addHandler));
                    futures.put(module, future);
                }

                for (Map.Entry<String, Future<OperationFailedException>> entry : futures.entrySet()) {
                    try {
                        OperationFailedException ofe = entry.getValue().get();
                        if (ofe != null) {
                            throw ofe;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw MESSAGES.moduleInitializationInterrupted(entry.getKey());
                    } catch (ExecutionException e) {
                        throw MESSAGES.failedInitializingModule(e, entry.getKey());
                    }
                }

                if (MGMT_OP_LOGGER.isDebugEnabled()) {
                    long elapsed = System.currentTimeMillis() - start;
                    MGMT_OP_LOGGER.debugf("Initialized extensions in [%d] ms", elapsed);
                }

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        };
    }

    private static class ExtensionInitializeTask implements Callable<OperationFailedException> {

        private final String module;
        private final ExtensionAddHandler addHandler;

        public ExtensionInitializeTask(String module, ExtensionAddHandler addHandler) {
            this.module = module;
            this.addHandler = addHandler;
        }

        @Override
        public OperationFailedException call() {
            OperationFailedException failure = null;
            try {
                addHandler.initializeExtension(module);
            } catch (OperationFailedException e) {
                failure = e;
            }
            return failure;
        }
    }
}
