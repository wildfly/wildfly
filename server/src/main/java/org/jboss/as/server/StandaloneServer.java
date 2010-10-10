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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.server.mgmt.ServerConfigurationPersisterImpl;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentManagerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentRepositoryImpl;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLMapper;

/**
 * The standalone server.
 *
 * @author Emanuel Muckenhuber
 */
public class StandaloneServer {

    private static final String STANDALONE_XML = "standalone.xml";
    private final StandardElementReaderRegistrar extensionRegistrar;

    static final Logger log = Logger.getLogger("org.jboss.as.server");

    private ServerModel config;

    private ServiceContainer serviceContainer;

    private final ServerEnvironment environment;

    protected StandaloneServer(ServerEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("bootstrapConfig is null");
        }
        this.environment = environment;
        extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();
    }

    public void start() throws ServerStartException {
        final File standalone = new File(getEnvironment().getServerConfigurationDir(), STANDALONE_XML);
        if(! standalone.isFile()) {
            throw new ServerStartException("File " + standalone.getAbsolutePath()  + " does not exist.");
        }
        if(! standalone.canWrite() ) {
            throw new ServerStartException("File " + standalone.getAbsolutePath()  + " is not writable.");
        }
        final List<AbstractServerModelUpdate<?>> updates = new ArrayList<AbstractServerModelUpdate<?>>();
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            extensionRegistrar.registerStandardStandaloneReaders(mapper);
            mapper.parseDocument(updates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(standalone))));
        } catch (Exception e) {
            throw new ServerStartException("Caught exception during processing of standalone.xml", e);
        }
        final ServerModel config = new ServerModel(null, 0);
        try {
            for(final AbstractServerModelUpdate<?> update : updates) {
                config.update(update);
            }
        } catch(UpdateFailedException e) {
            throw new ServerStartException("failed to process updates", e);
        }

        start(config);
        // TODO remove life thread
        new Thread() { {
                setName("Server Life Thread");
                setDaemon(false);
                setPriority(MIN_PRIORITY);
            }

            public void run() {
                for (;;)
                    try {
                        sleep(1000000L);
                    } catch (InterruptedException ignore) {
                        //
                    }
            }
        }.start();
    }

    ServerStartupListener.Callback createListenerCallback() {
        return new ServerStartupListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices) {
                if(serviceFailures.isEmpty()) {
                    log.infof("JBoss AS started in %dms. - Services [Total: %d, On-demand: %d. Started: %d]", elapsedTime, totalServices, onDemandServices, startedServices);
                } else {
                    final StringBuilder buff = new StringBuilder(String.format("JBoss AS server start failed. Attempted to start %d services in %dms", totalServices, elapsedTime));
                    buff.append("\nThe following services failed to start:\n");
                    for(Map.Entry<ServiceName, StartException> entry : serviceFailures.entrySet()) {
                        buff.append(String.format("\t%s => %s\n", entry.getKey(), entry.getValue().getMessage()));
                    }
                    log.error(buff.toString());
                }
            }
        };
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
     * @param config the server
     * @throws org.jboss.as.server.ServerStartException
     */
    void start(final ServerModel config) throws ServerStartException {
        if(config == null)  {
            throw new IllegalArgumentException("null standalone config");
        }
        this.config = config;
        log.infof("Starting server '%s'", environment.getProcessName());
        serviceContainer = ServiceContainer.Factory.create();

        final ServerStartupListener listener = new ServerStartupListener(createListenerCallback());

        try {
            // Activate subsystems
            final ServerStartBatchBuilder subsystemBatchBuilder = new ServerStartBatchBuilder(serviceContainer.batchBuilder(), listener);
            subsystemBatchBuilder.addListener(listener);

            // Activate core services not configured via ServerModel
            // TODO move creation of serviceContainer and installation of these
            // kinds of core services that aren't configured via the ServerModel
            // into whatever creates the ServerModel
            activateCoreServices(config, subsystemBatchBuilder);

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
                    config.activateDeployments(deploymentActivatorContext, serviceContainer);
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

    private void activateCoreServices(ServerModel serverConfiguration, ServerStartBatchBuilder batchBuilder) throws ServiceRegistryException {
        log.info("Activating core services");
        ServerEnvironmentService.addService(environment, batchBuilder);
        ServerDeploymentRepositoryImpl.addService(batchBuilder);
        ShutdownHandlerImpl.addService(batchBuilder);
        ServerConfigurationPersisterImpl.addService(serverConfiguration, batchBuilder);
        ServerDeploymentManagerImpl.addService(serverConfiguration, serviceContainer, batchBuilder);
    }
}

