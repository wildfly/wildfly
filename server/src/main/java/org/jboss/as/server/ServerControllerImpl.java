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

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServerControllerImpl implements ServerController, Service<ServerController> {
    private final ServiceContainer container;
    private final ServerModel serverModel;

    ServerControllerImpl(final ServerModel model, final ServiceContainer container) {
        this.serverModel = model;
        this.container = container;
    }

    /** {@inheritDoc} */
    public ServerModel getServerModel() {
        return serverModel;
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
        //
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
        //
    }
}
