/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

import org.jboss.as.deployment.DeploymentServiceListener;
import org.jboss.as.model.Standalone;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;

/**
 * The server base class.
 * 
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractServer {

	static final Logger log = Logger.getLogger("org.jboss.as.server");
	
	private Standalone config;
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
	public Standalone getConfig() {
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
	void start(final Standalone config) throws ServerStartException {
		if(config == null)  {
			throw new IllegalArgumentException("null standalone config");
		}
		this.config = config;
		log.infof("Starting server '%s'", config.getServerName());
        serviceContainer = ServiceContainer.Factory.create();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final DeploymentServiceListener listener = new DeploymentServiceListener(createDeploymentCallback());
        batchBuilder.addListener(listener);

        try {
            listener.startBatch();
            final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContextImpl(batchBuilder);
            // Activate
            config.activate(serviceActivatorContext);
            batchBuilder.install();
            listener.finishBatch();
            listener.finishDeployment();
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
	
	abstract DeploymentServiceListener.Callback createDeploymentCallback();
	
}

