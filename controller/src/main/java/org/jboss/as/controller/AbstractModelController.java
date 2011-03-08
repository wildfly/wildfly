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
package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass for {@link ModelController} implementations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelController implements ModelController {


    /** {@inheritDoc} */
    @Override
    public ModelNode execute(final ExecutionContext executionContext) {
        final ControllerTransactionContext transaction = null;
        return execute(executionContext, transaction);
    }

    protected ModelNode execute(final ExecutionContext executionContext, final ControllerTransactionContext transaction) {
        final AtomicInteger status = new AtomicInteger();
        final ModelNode finalResult = new ModelNode();
        // Make the "outcome" child come first
        finalResult.get(OUTCOME);
        // Ensure there is a "result" child even if we receive no fragments
        finalResult.get(RESULT);
        ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode fragment) {
                synchronized (finalResult) {
                    if (status.get() == 0) {
                        finalResult.get(RESULT).get(location).set(fragment);
                    }
                }
            }

            @Override
            public void handleResultComplete() {
                synchronized (finalResult) {
                    status.compareAndSet(0, 1);
                    finalResult.notify();
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                synchronized (finalResult) {
                    if (status.compareAndSet(0, 3)) {
                        if (failureDescription != null && failureDescription.isDefined()) {
                            finalResult.get(FAILURE_DESCRIPTION).set(failureDescription);
                        }
                    }
                    finalResult.notify();
                }
            }

            @Override
            public void handleCancellation() {
                synchronized (finalResult) {
                    if (status.compareAndSet(0, 2)) {
                        finalResult.remove(RESULT);
                    }
                    finalResult.notify();
                }
            }
        };
        final OperationResult handlerResult = transaction == null ? execute(executionContext, resultHandler) : execute(executionContext, resultHandler, transaction);
        boolean intr = false;
        try {
            synchronized (finalResult) {
                for (;;) {
                    try {
                        final int s = status.get();
                        switch (s) {
                            case 1: finalResult.get(OUTCOME).set(SUCCESS);
                                if(handlerResult.getCompensatingOperation() != null) {
                                   finalResult.get(COMPENSATING_OPERATION).set(handlerResult.getCompensatingOperation());
                                }
                                return finalResult;
                            case 2: finalResult.get(OUTCOME).set(CANCELLED);
                                throw new CancellationException();
                            case 3: finalResult.get(OUTCOME).set(FAILED);
                                if (!finalResult.hasDefined(RESULT)) {
                                    // Remove the undefined node
                                    finalResult.remove(RESULT);
                                }
                                return finalResult;
                        }
                        finalResult.wait();
                    } catch (final InterruptedException e) {
                        intr = true;
                        handlerResult.getCancellable().cancel();
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }

    }

    protected OperationResult execute(final ExecutionContext executionContext, final ResultHandler handler, final ControllerTransactionContext transaction) {
        throw new UnsupportedOperationException("Transactional operations are not supported");
    }

    /**
     * Get a failure result from a throwable exception.
     *
     * @param t the exception
     * @return the failure result
     */
    protected ModelNode getFailureResult(Throwable t) {
        final ModelNode node = new ModelNode();
        // todo - define this structure
        do {
            final String message = t.getLocalizedMessage();
            node.add(t.getClass().getName(), message != null ? message : "");
            t = t.getCause();
        } while (t != null);
        return node;
    }

}
