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

import org.jboss.as.model.ServerModel;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistryException;

/**
 * The server base class.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractServer {

    static final Logger log = Logger.getLogger("org.jboss.as.server");

    private ServerModel config;
    private ServiceContainer serviceContainer;
    private final ServerEnvironment environment;

    protected AbstractServer(final ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
    }

    /**
     * Get the server environment.
     *
     * @return the server environment
     */
    public ServerEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Get the standalone configuration.
     *
     * @return the standalone configuration
     */
    public ServerModel getConfig() {
        if(config == null) {
            throw new IllegalStateException("null configuration");
        }
        return config;
    }

    /**
     * Start the server.
     *
     * @throws ServerStartException
     */
    public abstract void start() throws ServerStartException;

    /**
     * Start the server.
     *
     * @param config the server
     * @throws ServerStartException
     */
    void start(final ServerModel config) throws ServerStartException {
        if(config == null)  {
            throw new IllegalArgumentException("null standalone config");
        }
        this.config = config;
        log.infof("Starting server '%s'", config.getServerName());
        serviceContainer = ServiceContainer.Factory.create();

        final ServerStartupListener listener = new ServerStartupListener(createListenerCallback());

        try {
            // Activate subsystems
            final ServerStartBatchBuilder subsystemBatchBuilder = new ServerStartBatchBuilder(serviceContainer.batchBuilder(), listener);
            subsystemBatchBuilder.addListener(listener);
            final ServiceActivatorContext subsystemActivatorContext = new ServiceActivatorContextImpl(subsystemBatchBuilder);
            config.activateSubsystems(subsystemActivatorContext);
            listener.startBatch(new Runnable() {
                @Override
                public void run() {
                    // Activate deployments once the first batch is complete.
                    final ServerStartBatchBuilder deploymentBatchBuilder = new ServerStartBatchBuilder(serviceContainer.batchBuilder(), listener);
                    deploymentBatchBuilder.addListener(listener);
                    final ServiceActivatorContext deploymentActivatorContext = new ServiceActivatorContextImpl(deploymentBatchBuilder);
                    listener.startBatch(null);
                    config.activateDeployments(deploymentActivatorContext);
                    listener.finish(); // We have finished adding everything for the server start
                    try {
                        deploymentBatchBuilder.install();
                        listener.finishBatch();
                    } catch (ServiceRegistryException e) {
                        throw new RuntimeException(e); // TODO: better exception handling.
                    }
                }
            });
            subsystemBatchBuilder.install();
            listener.finishBatch();
        } catch (Throwable t) {
            throw new ServerStartException("Failed to start server", t);
        }
    }

    /**
     * Stop the server.
     *
     */
    public void stop() {
        log.infof("Stopping server '%s'", config.getServerName());
        final ServiceContainer container = this.serviceContainer;
        if(container != null) {
            container.shutdown();
        }
        this.config = null;
        this.serviceContainer = null;
    }

    abstract ServerStartupListener.Callback createListenerCallback();

}

