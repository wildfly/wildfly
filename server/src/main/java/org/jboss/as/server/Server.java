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

/**
 * 
 */
package org.jboss.as.server;

import org.jboss.as.model.Standalone;
import org.jboss.as.process.ProcessManagerSlave;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistryException;


/**
 * An actual JBoss Application Server instance.
 * 
 * @author Brian Stansberry
 */
public class Server {

    private final ServerEnvironment environment;
    private ProcessManagerSlave processManagerSlave;
    private final ServerMessageHandler messageHandler = new ServerMessageHandler(this);
    private Standalone config;
    private ServiceContainer serviceContainer;

    public Server(ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        launchProcessManagerSlave();
    }
    
    public void start(Standalone config) throws ServerStartException {
        this.config = config;

        serviceContainer = ServiceContainer.Factory.create();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        config.activate(serviceContainer, batchBuilder);

        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new ServerStartException("Failed to install service batch", e);
        }
    }

    private void launchProcessManagerSlave() {
        this.processManagerSlave = new ProcessManagerSlave(environment.getStdin(), environment.getStdout(), messageHandler);
        Thread t = new Thread(this.processManagerSlave.getController(), "Server Process");
        t.setDaemon(true);
        t.start();
    }
    
    public void stop() {
        // FIXME implement start
    }
}
