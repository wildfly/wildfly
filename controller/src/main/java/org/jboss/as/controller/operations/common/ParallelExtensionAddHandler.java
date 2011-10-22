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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ParsedBootOp;
import org.jboss.as.controller.parsing.CommonXml;
import org.jboss.dmr.ModelNode;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParallelExtensionAddHandler implements OperationStepHandler {

    // TODO inject
    private final ExecutorService executor = CommonXml.bootExecutor;
    private final List<ParsedBootOp> extensionAdds = new ArrayList<ParsedBootOp>();

    public void addParsedOp(final ParsedBootOp op, final ExtensionAddHandler handler) {
        extensionAdds.add(new ParsedBootOp(op, handler));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        context.addStep(getParallelExtensionInitializeStep(), OperationContext.Stage.IMMEDIATE);

        for (ParsedBootOp op : extensionAdds) {
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
                        throw new RuntimeException(String.format("Interrupted awaiting initialization of module %s", entry.getKey()));
                    } catch (ExecutionException e) {
                        throw new RuntimeException(String.format("Failed initializing module %s", entry.getKey()), e);
                    }
                }

                long elapsed = System.currentTimeMillis() - start;
                System.out.println("Initialized extensions in " + elapsed + " ms");
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


    private static final class ParallelExtensionAddhreadFactory implements ThreadFactory {

        private int threadCount;
        @Override
        public Thread newThread(Runnable r) {

            Thread t = new Thread(r, ParallelExtensionAddhreadFactory.class.getSimpleName() + "-" + (++threadCount));
            t.setDaemon(true);
            return t;
        }
    }
}
