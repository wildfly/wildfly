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

package org.jboss.as.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.protocol.mgmt.ManagementException;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.server.mgmt.ServerUpdateController;
import org.jboss.as.server.mgmt.ServerUpdateController.ServerUpdateCommitHandler;
import org.jboss.as.server.mgmt.ServerUpdateController.Status;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServerControllerImpl implements ServerController, Service<ServerController> {

    private final InjectedValue<ServerConfigurationPersister> configurationPersisterValue = new InjectedValue<ServerConfigurationPersister>();
    private final InjectedValue<ExecutorService> executorValue = new InjectedValue<ExecutorService>();

    private final ServiceContainer container;
    private final ServerModel serverModel;
    private final boolean standalone;
    private ServerConfigurationPersister configurationPersister;
    private ExecutorService executor;


    ServerControllerImpl(final ServerModel model, final ServiceContainer container, final boolean standalone) {
        this.serverModel = model;
        this.container = container;
        this.standalone = standalone;
    }

    /** {@inheritDoc} */
    public ServerModel getServerModel() {
        return serverModel;
    }

    @Override
    public List<UpdateResultHandlerResponse<?>> applyUpdates(List<AbstractServerModelUpdate<?>> updates,
            boolean rollbackOnFailure, boolean modelOnly) {

        if (modelOnly && !standalone) {
            throw new IllegalStateException("model-only updates are not valid on a domain-based server");
        }

        List<UpdateResultHandlerResponse<?>> results = new ArrayList<UpdateResultHandlerResponse<?>>(updates.size());

        final CountDownLatch latch = new CountDownLatch(1);
        ServerUpdateCommitHandlerImpl handler = new ServerUpdateCommitHandlerImpl(results, latch);
        final ServerUpdateController controller = new ServerUpdateController(getServerModel(),
                container, executor, handler, rollbackOnFailure, modelOnly);

        for(int i = 0; i < updates.size(); i++) {
            controller.addServerModelUpdate(updates.get(i), handler, Integer.valueOf(i));
        }

        controller.executeUpdates();
        try {
            latch.await();
        } catch(Exception e) {
            throw new ManagementException("failed to execute updates", e);
        }

        return results;
    }

    public <R, P> void update(final AbstractServerModelUpdate<R> update, final UpdateResultHandler<R, P> resultHandler, final P param) {
        final UpdateContextImpl updateContext = new UpdateContextImpl(container.batchBuilder(), container);
        synchronized (serverModel) {
            try {
                serverModel.update(update);
            } catch (UpdateFailedException e) {
                resultHandler.handleFailure(e, param);
                return;
            }
        }
        update.applyUpdate(updateContext, resultHandler, param);
    }

    public void shutdown() {
        container.shutdown();
    }

    public InjectedValue<ServerConfigurationPersister> getConfigurationPersisterValue() {
        return configurationPersisterValue;
    }

    public InjectedValue<ExecutorService> getExecutorValue() {
        return executorValue;
    }

    final class UpdateContextImpl implements UpdateContext {

        private final BatchBuilder batchBuilder;
        private final ServiceContainer serviceContainer;

        UpdateContextImpl(final BatchBuilder batchBuilder, final ServiceContainer serviceContainer) {
            this.batchBuilder = batchBuilder;
            this.serviceContainer = serviceContainer;
        }

        public BatchBuilder getBatchBuilder() {
            return batchBuilder;
        }

        public ServiceContainer getServiceContainer() {
            return serviceContainer;
        }
    }

    /** {@inheritDoc} */
    public ServerController getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        try {
            configurationPersister = configurationPersisterValue.getValue();
            executor = executorValue.getValue();
        } catch (IllegalStateException e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
        //
    }

    private class ServerUpdateCommitHandlerImpl implements ServerUpdateCommitHandler, UpdateResultHandler<Object, Integer> {

        private final List<UpdateResultHandlerResponse<?>> responses;
        private final CountDownLatch latch;

        public ServerUpdateCommitHandlerImpl(List<UpdateResultHandlerResponse<?>> responses, CountDownLatch latch) {
            this.responses = responses;
            this.latch = latch;
        }

        @Override
        public void handleUpdateCommit(ServerUpdateController controller, Status priorStatus) {
            configurationPersister.configurationModified();
            latch.countDown();
        }

        @Override
        public void handleCancellation(Integer param) {
            responses.set(param, UpdateResultHandlerResponse.createCancellationResponse());
        }

        @Override
        public void handleFailure(Throwable cause, Integer param) {
            responses.set(param, UpdateResultHandlerResponse.createFailureResponse(cause));
        }

        @Override
        public void handleRollbackCancellation(Integer param) {
            UpdateResultHandlerResponse<?> rsp = responses.get(param);
            if (rsp == null) {
                throw new IllegalStateException("No response assoicated with index " + param);
            }
            responses.set(param, UpdateResultHandlerResponse.createRollbackCancelledResponse(rsp));
        }

        @Override
        public void handleRollbackFailure(Throwable cause, Integer param) {
            UpdateResultHandlerResponse<?> rsp = responses.get(param);
            if (rsp == null) {
                throw new IllegalStateException("No response assoicated with index " + param);
            }
            responses.set(param, UpdateResultHandlerResponse.createRollbackFailedResponse(rsp, cause));
        }

        @Override
        public void handleRollbackSuccess(Integer param) {
            UpdateResultHandlerResponse<?> rsp = responses.get(param);
            if (rsp == null) {
                throw new IllegalStateException("No response assoicated with index " + param);
            }
            responses.set(param, UpdateResultHandlerResponse.createRollbackResponse(rsp));
        }

        @Override
        public void handleRollbackTimeout(Integer param) {
            UpdateResultHandlerResponse<?> rsp = responses.get(param);
            if (rsp == null) {
                throw new IllegalStateException("No response assoicated with index " + param);
            }
            responses.set(param, UpdateResultHandlerResponse.createRollbackTimedOutResponse(rsp));
        }

        @Override
        public void handleSuccess(Object result, Integer param) {
            responses.set(param, UpdateResultHandlerResponse.createSuccessResponse(result));
        }

        @Override
        public void handleTimeout(Integer param) {
            responses.set(param, UpdateResultHandlerResponse.createTimeoutResponse());
        }
    }
}
