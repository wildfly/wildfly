/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.client.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.dmr.ModelNode;

/**
 * {@link ServerDeploymentManager} the uses a {@link ModelController}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class ModelControllerServerDeploymentManager extends AbstractServerDeploymentManager {

    private final ModelController client;

    public ModelControllerServerDeploymentManager(final ModelController client) {
        this.client = client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Future<ModelNode> executeOperation(ModelNode operation) {
        Handler handler = new Handler();
        Cancellable c = client.execute(operation, handler.resultHandler);
        handler.setCancellable(c);
        return handler;
    }

    private static class Handler implements Future<ModelNode> {

        private enum State {
            RUNNING, CANCELLED, DONE
        }

        private AtomicReference<State> state = new AtomicReference<State>(State.RUNNING);
        private final Thread runner = Thread.currentThread();
        private final ModelNode result = new ModelNode();
        private Cancellable cancellable;
        private final AtomicReference<Exception> exception = new AtomicReference<Exception>();
        private final ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(String[] location, ModelNode fragment) {
                if (state.get() == State.RUNNING) {
                    result.get(location).set(fragment);
                }
            }

            @Override
            public void handleResultComplete(ModelNode compensatingOperation) {
                state.compareAndSet(State.RUNNING, State.DONE);
                synchronized (Handler.this) {
                    Handler.this.notifyAll();
                }
            }

            @Override
            public void handleCancellation() {
                state.compareAndSet(State.RUNNING, State.CANCELLED);
                synchronized (Handler.this) {
                    Handler.this.notifyAll();
                }
            }

            @Override
            public void handleFailed(ModelNode node) {
                exception.compareAndSet(null, new OperationFailedException(node));
                state.compareAndSet(State.RUNNING, State.DONE);
                synchronized (Handler.this) {
                    Handler.this.notifyAll();
                }
            }
        };

        void setCancellable(Cancellable c) {
            this.cancellable = c;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = false;
            if (state.get() == State.RUNNING) {
                if (cancellable.cancel()) {
                    cancelled = state.compareAndSet(State.RUNNING, State.CANCELLED);
                }

                if (!cancelled) {
                    if (mayInterruptIfRunning) {
                        runner.interrupt();
                    }
                    if (state.compareAndSet(State.RUNNING, State.DONE)) {
                        exception.set(new CancellationException());
                    }
                }
            }
            synchronized (this) {
                notifyAll();
            }
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            return state.get() == State.CANCELLED;
        }

        @Override
        public boolean isDone() {
            return state.get() != State.RUNNING;
        }

        @Override
        public ModelNode get() throws InterruptedException, ExecutionException {
            synchronized (this) {
                while (!isDone()) {
                    wait();
                }

                return getResult();
            }
        }

        @Override
        public ModelNode get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
            long toWait = unit.toMillis(timeout);
            long expire = System.currentTimeMillis() + toWait;
            synchronized (this) {
                while (!isDone()) {
                    wait(toWait);
                    if (!isDone()) {
                        long now = System.currentTimeMillis();
                        if (now >= expire) {
                            throw new TimeoutException();
                        }
                        toWait = expire - now;
                    }
                }
                return getResult();
            }
        }

        private ModelNode getResult() throws ExecutionException {
            Exception e = exception.get();
            if (e instanceof ExecutionException) {
                throw (ExecutionException) e;
            } else if (e instanceof CancellationException) {
                throw (CancellationException) e;
            } else if (e != null) {
                throw new ExecutionException(e);
            } else if (state.get() == State.CANCELLED) {
                throw new CancellationException();
            } else {
                return result;
            }
        }

    }

}
