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
import org.jboss.as.server.mgmt.ShutdownHandler;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.services.net.SocketBindingManager;
import org.jboss.as.server.services.net.SocketBindingManagerService;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.version.Version;
import org.jboss.logging.Logger;
import org.jboss.msc.service.MultipleRemoveListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TrackingServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ApplicationServerService implements Service<Void> {

    private static final Logger log = Logger.getLogger("org.jboss.as");
    private final List<ServiceActivator> extraServices;
    private final Bootstrap.Configuration configuration;
    private List<ServiceName> services;

    ApplicationServerService(final List<ServiceActivator> extraServices, final Bootstrap.Configuration configuration) {
        this.extraServices = extraServices;
        this.configuration = configuration;
    }

    public synchronized void start(final StartContext context) throws StartException {
        log.infof("JBoss AS %s \"%s\" starting", Version.AS_VERSION, Version.AS_RELEASE_CODENAME);
        final ServiceContainer container = context.getController().getServiceContainer();
        final TrackingServiceTarget serviceTarget = new TrackingServiceTarget(container.subTarget());
        serviceTarget.addDependency(context.getController().getName());
        Bootstrap.Configuration configuration = this.configuration;
        serviceTarget
                .addService(Services.JBOSS_SERVER_CONTROLLER, new ServerControllerService(configuration))
                .install();
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();
        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            public ServiceTarget getServiceTarget() {
                return serviceTarget;
            }

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

        // Socket binding manager
        serviceTarget.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
            new SocketBindingManagerService(configuration.getPortOffset()))
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();
        services = new ArrayList<ServiceName>(serviceTarget.getSet());
        if (log.isDebugEnabled()) {
            final long nanos = context.getElapsedTime();
            log.debugf("JBoss AS root service started in %d.%06d ms", Long.valueOf(nanos / 1000000L), Long.valueOf(nanos % 1000000L));
        }
    }

    public synchronized void stop(final StopContext context) {
        log.infof("Shutdown requested; stopping all services");
        context.asynchronous();
        final ServiceContainer container = context.getController().getServiceContainer();
        final MultipleRemoveListener<Runnable> listener = MultipleRemoveListener.create(new Runnable() {
            public void run() {
                context.complete();
                log.infof("JBoss AS %s \"%s\" stopped in %dms", Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
            }
        });
        for (ServiceName name : services) {
            ServiceController<?> service = container.getService(name);
            if (service != null) {
                service.addListener(listener);
                service.setMode(ServiceController.Mode.REMOVE);
            }
        }
        listener.done();
    }

    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}
