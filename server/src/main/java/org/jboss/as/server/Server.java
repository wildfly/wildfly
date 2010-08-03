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
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.TimingServiceListener;


/**
 * An actual JBoss Application Server instance.
 * 
 * @author Brian Stansberry
 * @author John E. Bailey
 */
public class Server {
    private static final Logger logger = Logger.getLogger("org.jboss.as.server");
    private final ServerEnvironment environment;
    private ProcessManagerSlave processManagerSlave;
    private final MessageHandler messageHandler = new MessageHandler(this);
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
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            @Override
            public void run() {
                logger.infof("JBossAS started...."); // TODO: Get access to service status ex.  [%d services in %d seconds]", listener.getTotalCount(), listener.getElapsedTime());
            }
        });
        batchBuilder.addListener(listener);

        config.activate(serviceContainer, batchBuilder);

        try {
            batchBuilder.install();
            listener.finishBatch();
        } catch (ServiceRegistryException e) {
            throw new ServerStartException("Failed to install service batch", e);
        }
    }

    private void launchProcessManagerSlave() {
        this.processManagerSlave = ProcessManagerSlaveFactory.getInstance().getProcessManagerSlave(environment, messageHandler);
        Thread t = new Thread(this.processManagerSlave.getController(), "Server Process");
        t.setDaemon(true);
        t.start();
    }
    
    public void stop() {
        // FIXME implement start
    }
}
