/**
 *
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

import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

/**
 * Helper class for converting asynchronous operation handling into synchronous.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SynchronousOperationSupport<T> {

    public interface AsynchronousOperationController<T> {

        /**
         * Execute the given operation as per {@link ModelController#execute(Operation, ResultHandler)
         *
         * @param operation the operation
         * @param resultHandler the result handler
         * @param handback handback object passed in to {@link SynchronousOperationSupport#execute(Operation, Object, AsynchronousOperationController)}
         * @return
         */
        OperationResult execute(Operation operation, ResultHandler resultHandler, T handback);
    }


    public static <T> ModelNode execute(final Operation operation, T handback, AsynchronousOperationController<T> controller) {
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
        final OperationResult handlerResult = controller.execute(operation, resultHandler, handback);
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
}
