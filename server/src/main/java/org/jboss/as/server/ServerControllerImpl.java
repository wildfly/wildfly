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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServerControllerImpl implements ServerController {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ServiceContainer container;
    private final ServiceRegistry registry;
    private final ServerModel serverModel;
    private final ServerEnvironment serverEnvironment;
    private final ServerConfigurationPersister configurationPersister;
    private final ExecutorService executor;

    ServerControllerImpl(final ServerModel serverModel, final ServiceContainer container, final ServerEnvironment serverEnvironment, final ServerConfigurationPersister configurationPersister, final ExecutorService executor) {
        this.serverModel = serverModel;
        this.container = container;
        this.serverEnvironment = serverEnvironment;
        this.configurationPersister = configurationPersister;
        this.executor = executor;
        registry = new DelegatingServiceRegistry(container);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public ServerModel getServerModel() {
        return serverModel;
    }

    /** {@inheritDoc} */
    @Override
    public ServerEnvironment getServerEnvironment() {
        return serverEnvironment;
    }

    @Override
    public void execute(final ModelNode request, final Queue<ModelNode> responseQueue) {
        // FIXME implemenet execute(ModelNode, Queue<ModelNode)
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    @Override
    public List<UpdateResultHandlerResponse<?>> applyUpdates(final List<AbstractServerModelUpdate<?>> updates,
            final boolean rollbackOnFailure, final boolean modelOnly) {

        final int count = updates.size();
        log.debugf("Received %d updates", Integer.valueOf(count));

        final List<UpdateResultHandlerResponse<?>> results = new ArrayList<UpdateResultHandlerResponse<?>>(count);
        if (modelOnly && ! serverEnvironment.isStandalone()) {
            final Exception e = new IllegalStateException("Update sets that only affect the configuration and not the runtime are not valid on a domain-based server");
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < count; i++) {
                results.add(UpdateResultHandlerResponse.createFailureResponse(e));
            }
            return results;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final ServerUpdateCommitHandlerImpl handler = new ServerUpdateCommitHandlerImpl(results, count, latch);
        final ServerUpdateController controller = new ServerUpdateController(serverModel,
                container, executor, handler, rollbackOnFailure, !modelOnly);

        for(int i = 0; i < count; i++) {
            controller.addServerModelUpdate(updates.get(i), handler, Integer.valueOf(i));
        }

        controller.executeUpdates();
        log.debugf("Executed %d updates", Integer.valueOf(updates.size()));
        try {
            latch.await();
        } catch(final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ManagementException("failed to execute updates", e);
        }

        return results;
    }

    @Override
    public <R, P> void update(final AbstractServerModelUpdate<R> update, final UpdateResultHandler<R, P> resultHandler, final P param) {
        final UpdateContextImpl updateContext = new UpdateContextImpl(container.batchBuilder(), registry);
        synchronized (serverModel) {
            try {
                serverModel.update(update);
            } catch (final UpdateFailedException e) {
                resultHandler.handleFailure(e, param);
                return;
            } catch (final Throwable t) {
                resultHandler.handleFailure(t, param);
                return;
            }
        }
        update.applyUpdate(updateContext, resultHandler, param);
    }

    @Override
    public void shutdown() {
        container.shutdown();
    }

    @Override
    public boolean isShutdownComplete() {
        return container.isShutdownComplete();
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        container.awaitTermination();
    }

    @Override
    public void awaitTermination(final long time, final TimeUnit unit) throws InterruptedException {
        container.awaitTermination(time, unit);
    }

    final class UpdateContextImpl implements UpdateContext {

        private final ServiceTarget serviceTarget;
        private final ServiceRegistry serviceRegistry;

        UpdateContextImpl(final ServiceTarget serviceTarget, final ServiceRegistry registry) {
            this.serviceTarget = serviceTarget;
            serviceRegistry = registry;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return serviceRegistry;
        }

        @Override
        public ServiceTarget getServiceTarget() {
            return serviceTarget;
        }

        public ServiceContainer getServiceContainer() {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    public ServerController getValue() throws IllegalStateException {
        return this;
    }

    private class ServerUpdateCommitHandlerImpl implements ServerUpdateCommitHandler, UpdateResultHandler<Object, Integer> {

        private final int count;
        private final Map<Integer, UpdateResultHandlerResponse<?>> map = new ConcurrentHashMap<Integer, UpdateResultHandlerResponse<?>>();
        private final List<UpdateResultHandlerResponse<?>> responses;
        private final CountDownLatch latch;

        public ServerUpdateCommitHandlerImpl(final List<UpdateResultHandlerResponse<?>> responses, final int count, final CountDownLatch latch) {
            this.responses = responses;
            this.count = count;
            this.latch = latch;
        }

        @Override
        public void handleUpdateCommit(final ServerUpdateController controller, final Status priorStatus) {
            log.tracef("Committed with prior status %s", priorStatus);
            configurationPersister.persist(ServerControllerImpl.this, serverModel);

            for (int i = 0; i < count; i++) {
                responses.add(map.get(Integer.valueOf(i)));
            }
            latch.countDown();
        }

        @Override
        public void handleCancellation(final Integer param) {
            log.tracef("Update %d cancelled", param);
            map.put(param, UpdateResultHandlerResponse.createCancellationResponse());
        }

        @Override
        public void handleFailure(final Throwable cause, final Integer param) {
            log.tracef("Update %d failed with %s", param, cause);
            map.put(param, UpdateResultHandlerResponse.createFailureResponse(cause));
        }

        @Override
        public void handleRollbackCancellation(final Integer param) {
            log.tracef("Update %d rollback cancelled", param);
            final UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackCancelledResponse(rsp));
        }

        @Override
        public void handleRollbackFailure(final Throwable cause, final Integer param) {
            log.tracef("Update %d rollback failed with %s", param, cause);
            final UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackFailedResponse(rsp, cause));
        }

        @Override
        public void handleRollbackSuccess(final Integer param) {
            log.tracef("Update %d rolled back", param);
            final UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackResponse(rsp));
        }

        @Override
        public void handleRollbackTimeout(final Integer param) {
            log.tracef("Update %d rollback timed out", param);
            final UpdateResultHandlerResponse<?> rsp = map.get(param);
            if (rsp == null) {
                log.warn("No response associated with index " + param);
                return;
            }
            map.put(param, UpdateResultHandlerResponse.createRollbackTimedOutResponse(rsp));
        }

        @Override
        public void handleSuccess(final Object result, final Integer param) {
            log.tracef("Update %d succeeded", param);
            map.put(param, UpdateResultHandlerResponse.createSuccessResponse(result));
        }

        @Override
        public void handleTimeout(final Integer param) {
            log.tracef("Update %d timed out", param);
            responses.set(param.intValue(), UpdateResultHandlerResponse.createTimeoutResponse());
        }
    }
}
