/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.server.ServerLogger.AS_ROOT_LOGGER;
import static org.jboss.as.server.ServerLogger.CONFIG_LOGGER;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.server.deployment.repository.impl.ContentRepositoryImpl;
import org.jboss.as.server.deployment.repository.impl.ServerDeploymentRepositoryImpl;
import org.jboss.as.server.mgmt.ShutdownHandler;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ModuleIndexService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.version.Version;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.AsyncFuture;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ApplicationServerService implements Service<AsyncFuture<ServiceContainer>> {

    private final List<ServiceActivator> extraServices;
    private final Bootstrap.Configuration configuration;
    private final ControlledProcessState processState;
    private volatile FutureServiceContainer futureContainer;
    private volatile long startTime;

    ApplicationServerService(final List<ServiceActivator> extraServices, final Bootstrap.Configuration configuration) {
        this.extraServices = extraServices;
        this.configuration = configuration;
        startTime = configuration.getStartTime();
        processState = new ControlledProcessState(configuration.getServerEnvironment().isStandalone());
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        processState.setStarting();

        final Bootstrap.Configuration configuration = this.configuration;
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();

        // Install the environment before doing anything
        serverEnvironment.install();

        AS_ROOT_LOGGER.serverStarting(Version.AS_VERSION, Version.AS_RELEASE_CODENAME);
        if (CONFIG_LOGGER.isDebugEnabled()) {
            final Properties properties = System.getProperties();
            String newLine = properties.getProperty("line.separator");
            if (newLine == null) {
                newLine = "\n\t";
            } else {
                newLine += "\t";
            }
            final StringBuilder b = new StringBuilder(8192);
            b.append("Configured system properties:");
            for (String property : new TreeSet<String>(properties.stringPropertyNames())) {
                b.append(newLine).append(property).append(" = ").append(properties.getProperty(property, "<undefined>"));
            }
            CONFIG_LOGGER.debug(b);
            if (CONFIG_LOGGER.isTraceEnabled()) {
                b.setLength(0);
                final Map<String,String> env = System.getenv();
                b.append("Configured system environment:");
                for (String key : new TreeSet<String>(env.keySet())) {
                    b.append(newLine).append(key).append(" = ").append(env.get(key));
                }
                CONFIG_LOGGER.trace(b);
            } else {
                final String javaOpts = System.getenv().get("JAVA_OPTS");
                if (javaOpts == null || javaOpts.isEmpty()) {
                    CONFIG_LOGGER.debug("JAVA_OPTS could not be determined.");
                } else {
                    CONFIG_LOGGER.debugf("JAVA_OPTS=\"%s\"", javaOpts);
                }
            }
        }
        final ServiceTarget serviceTarget = context.getChildTarget();
        final ServiceController<?> myController = context.getController();
        final ServiceContainer container = myController.getServiceContainer();
        futureContainer = new FutureServiceContainer();

        long startTime = this.startTime;
        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        } else {
            this.startTime = -1;
        }

        CurrentServiceContainer.setServiceContainer(context.getController().getServiceContainer());

        final BootstrapListener bootstrapListener = new BootstrapListener(container, startTime, serviceTarget, futureContainer, configuration);
        serviceTarget.addListener(ServiceListener.Inheritance.ALL, bootstrapListener);
        myController.addListener(bootstrapListener);
        ContentRepositoryImpl contentRepository = ContentRepositoryImpl.addService(serviceTarget, serverEnvironment.getServerDeployDir());
        ServerDeploymentRepositoryImpl.addService(serviceTarget, contentRepository);
        ServiceModuleLoader.addService(serviceTarget, configuration);
        ExternalModuleService.addService(serviceTarget);
        ModuleIndexService.addService(serviceTarget);
        ServerService.addService(serviceTarget, configuration, processState, bootstrapListener, new ServerRuntimeVaultReader());
        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            @Override
            public ServiceTarget getServiceTarget() {
                return serviceTarget;
            }

            @Override
            public ServiceRegistry getServiceRegistry() {
                return container;
            }
        };

        for(ServiceActivator activator : extraServices) {
            activator.activate(serviceActivatorContext);
        }

        // TODO: decide the fate of these

        // Graceful shutdown
        serviceTarget.addService(ShutdownHandler.SERVICE_NAME, new ShutdownHandlerImpl()).install();

        // Add server environment
        ServerEnvironmentService.addService(serverEnvironment, serviceTarget);

        // Add environment paths
        AbsolutePathService.addService(ServerEnvironment.HOME_DIR, serverEnvironment.getHomeDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(ServerEnvironment.SERVER_BASE_DIR, serverEnvironment.getServerBaseDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(ServerEnvironment.SERVER_CONFIG_DIR, serverEnvironment.getServerConfigurationDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(ServerEnvironment.SERVER_DATA_DIR, serverEnvironment.getServerDataDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(ServerEnvironment.SERVER_LOG_DIR, serverEnvironment.getServerLogDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(ServerEnvironment.SERVER_TEMP_DIR, serverEnvironment.getServerTempDir().getAbsolutePath(), serviceTarget);

        // Add system paths
        AbsolutePathService.addService("user.dir", System.getProperty("user.dir"), serviceTarget);
        AbsolutePathService.addService("user.home", System.getProperty("user.home"), serviceTarget);
        AbsolutePathService.addService("java.home", System.getProperty("java.home"), serviceTarget);

        // BES 2011/06/11 -- moved this to AbstractControllerService.start()
//        processState.setRunning();

        if (AS_ROOT_LOGGER.isDebugEnabled()) {
            final long nanos = context.getElapsedTime();
            AS_ROOT_LOGGER.debugf("JBoss AS root service started in %d.%06d ms", Long.valueOf(nanos / 1000000L), Long.valueOf(nanos % 1000000L));
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        processState.setStopping();
        CurrentServiceContainer.setServiceContainer(null);
        AS_ROOT_LOGGER.serverStopped(Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
    }

    @Override
    public AsyncFuture<ServiceContainer> getValue() throws IllegalStateException, IllegalArgumentException {
        return futureContainer;
    }
}
