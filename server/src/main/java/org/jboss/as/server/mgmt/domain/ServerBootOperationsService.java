/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.mgmt.domain;

import org.jboss.as.controller.ModelController;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.AsyncFutureTask;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service triggering the registration at the local host-controller and resolving the boot operations.
 * This service depends on the {@code HostControllerConnection} and {@code ModelController}, relying
 * on the fact that the boot() process happens in a different thread and the controller service is already
 * seen as started. The {@code Future} is used in the {@linkplain org.jboss.as.server.Bootstrap.ConfigurationPersisterFactory}
 * to block on the registration result.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerBootOperationsService implements Service<Void> {

    final InjectedValue<ModelController> serverController = new InjectedValue<ModelController>();
    final InjectedValue<HostControllerClient> clientInjector = new InjectedValue<HostControllerClient>();
    final InjectedValue<Executor> executorInjector = new InjectedValue<Executor>();

    private FutureBootUpdates future = new FutureBootUpdates();

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final HostControllerClient client = clientInjector.getValue();
        final ModelController controller = serverController.getValue();
        final Executor executor = executorInjector.getValue();
        context.asynchronous();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    client.resolveBootUpdates(controller, future);
                    context.complete();
                } catch (Exception e) {
                    future.failed(e);
                    context.failed(new StartException(e));
                }
            }
        });
    }

    @Override
    public synchronized void stop(final StopContext context) {
        final FutureBootUpdates updates = this.future;
        this.future = new FutureBootUpdates();
        if(! updates.isDone()) {
            updates.cancelled();
        }
    }

    public Future<ModelNode> getFutureResult() {
        return new Future<ModelNode>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return getFutureTask().cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return getFutureTask().isCancelled();
            }

            @Override
            public boolean isDone() {
                return getFutureTask().isDone();
            }

            @Override
            public ModelNode get() throws InterruptedException, ExecutionException {
                return getFutureTask().get();
            }

            @Override
            public ModelNode get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return getFutureTask().get(timeout, unit);
            }
        };
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    private synchronized Future<ModelNode> getFutureTask() {
        return future;
    }

    public InjectedValue<HostControllerClient> getClientInjector() {
        return clientInjector;
    }

    public InjectedValue<ModelController> getServerController() {
        return serverController;
    }

    public InjectedValue<Executor> getExecutorInjector() {
        return executorInjector;
    }

    private static class FutureBootUpdates extends AsyncFutureTask<ModelNode> implements ActiveOperation.CompletedCallback<ModelNode> {

        private FutureBootUpdates() {
            super(null);
        }

        @Override
        public void completed(final ModelNode result) {
            setResult(result);
        }

        @Override
        public void failed(Exception e) {
            setFailed(e);
        }

        @Override
        public void cancelled() {
            setCancelled();
        }
    }

}
