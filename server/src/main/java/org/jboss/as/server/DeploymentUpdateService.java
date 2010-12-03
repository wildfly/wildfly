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

import java.util.List;
import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service used to execute deployment updates once required deployment dependencies are available.
 *
 * @author John Bailey
 */
public class DeploymentUpdateService implements Service<Void> {
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment", "updates");
    private final List<AbstractServerModelUpdate<?>> updates;
    private final ServerStartupListener serverStartupListener;

    static final void addService(final BatchBuilder batchBuilder, final List<AbstractServerModelUpdate<?>> updates, final ServerStartupListener serverStartupListener) {
        batchBuilder.addService(SERVICE_NAME, new DeploymentUpdateService(updates, serverStartupListener))
            .addDependency(ServerDeploymentRepository.SERVICE_NAME);
    }

    public DeploymentUpdateService(final List<AbstractServerModelUpdate<?>> updates, final ServerStartupListener serverStartupListener) {
        this.updates = updates;
        this.serverStartupListener = serverStartupListener;
    }

    public void start(StartContext context) throws StartException {
        final ServiceContainer serviceContainer = context.getController().getServiceContainer();

        final ServerStartBatchBuilder batchBuilder = new ServerStartBatchBuilder(serviceContainer.batchBuilder(), serverStartupListener);
        batchBuilder.addListener(serverStartupListener);

        final BootUpdateContext updateContext = new BootUpdateContext() {
            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }

            public ServiceContainer getServiceContainer() {
                return serviceContainer;
            }

            public void addDeploymentProcessor(DeploymentUnitProcessor processor, long priority) {
                throw new UnsupportedOperationException("Deployments are not allowed to add deployment unit processors");
            }
        };

        // Deployment chain should be configured now..
        for (AbstractServerModelUpdate<?> update : updates) {
            if(update.isDeploymentUpdate()) {
                update.applyUpdateBootAction(updateContext);
            }
        }

        try {
            batchBuilder.install();
            serverStartupListener.finish();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to install deployment services", e);
        }
    }

    public void stop(StopContext context) {
    }

    public Void getValue() throws IllegalStateException {
        return null;
    }
}
