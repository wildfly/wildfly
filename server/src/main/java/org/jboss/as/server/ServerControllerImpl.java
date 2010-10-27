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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.jboss.logging.Logger;
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

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

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

        int count = updates.size();
        log.debugf("Received %s updates", count);

        List<UpdateResultHandlerResponse<?>> results = new ArrayList<UpdateResultHandlerResponse<?>>(count);
        if (modelOnly && !standalone) {
            Exception e = new IllegalStateException("Update sets that only affect the configuration and not the runtime are not valid on a domain-based server");
            for (int i = 0; i < count; i++) {
                results.add(UpdateResultHandlerResponse.createFailureResponse(e));
            }
            return results;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        ServerUpdateCommitHandlerImpl handler = new ServerUpdateCommitHandlerImpl(results, count, latch);
        final ServerUpdateController controller = new ServerUpdateController(getServerModel(),
                container, executor, handler, rollbackOnFailure, !modelOnly);

        for(int i = 0; i < count; i++) {
            controller.addServerModelUpdate(updates.get(i), handler, Integer.valueOf(i));
        }

        controller.executeUpdates();
        log.debugf("Executed %s updates", updates.size());
        try {
            latch.await();
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
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

        private final int count;
        private final Map<Integer, UpdateResultHandlerResponse<?>> map = new ConcurrentHashMap<Integer, UpdateResultHandlerResponse<?>>();
        private final List<UpdateResultHandlerResponse<?>> responses;
        private final CountDownLatch latch;
        private final boolean trace = log.isTraceEnabled();

        public ServerUpdateCommitHandlerImpl(List<UpdateResultHandlerResponse<?>> responses, int count, CountDownLatch latch) {
            this.responses = responses;
            this.count = count;
            this.latch = latch;
        }

        @Override
        public void handleUpdateCommit(ServerUpdateController controller, Status priorStatus) {
            log.tracef("Committed with prior status %s", priorStatus);
            configurationPersister.configurationModified();

            for (int i = 0; i < count; i++) {
                responses.add(map.get(Integer.valueOf(i)));
            }
            latch.countDown();
        }

        @Override
        public void handleCancellation(Integer param) {
            log.tracef("Update %s cancelled", param);
            map.put(param, UpdateResultHandlerResponse.createCancellationResponse());
        }

        @Override
        public void handleFailure(Throwable cause, Integer param) {
            log.tracef("Update %s failed with %s", param, cause);
            map.put(param, UpdateResultHandlerResponse.createFailureResponse(cause));
        }

        @Override
        public void handleRollbackCancellation(Integer param) {
            log.tracef("Update %s rollback cancelled", param);
            UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackCancelledResponse(rsp));
        }

        @Override
        public void handleRollbackFailure(Throwable cause, Integer param) {
            log.tracef("Update %s rollback failed with %s", param, cause);
            UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackFailedResponse(rsp, cause));
        }

        @Override
        public void handleRollbackSuccess(Integer param) {
            log.tracef("Update %s rolled back", param);
            UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackResponse(rsp));
        }

        @Override
        public void handleRollbackTimeout(Integer param) {
            log.tracef("Update %s rollback timed out", param);
            UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackTimedOutResponse(rsp));
        }

        @Override
        public void handleSuccess(Object result, Integer param) {
            log.tracef("Update %s succeeded", param);
            map.put(param, UpdateResultHandlerResponse.createSuccessResponse(result));
        }

        @Override
        public void handleTimeout(Integer param) {
            log.tracef("Update %s timed out", param);
            responses.set(param, UpdateResultHandlerResponse.createTimeoutResponse());
        }
    }
}
