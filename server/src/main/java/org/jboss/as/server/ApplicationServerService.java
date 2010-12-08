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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.ServerModel;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TrackingServiceTarget;

/**
 * The root service of the JBoss Application Server.  Stopping this
 * service will shut down the whole works.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ApplicationServerService implements Service<ServerController> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final Bootstrap.Configuration configuration;
    private final ServerConfigurationPersister persister;

    // mutable state
    private ServerController serverController;
    private Set<ServiceName> bootServices;

    public ApplicationServerService(final Bootstrap.Configuration configuration, final ServerConfigurationPersister persister) {
        this.configuration = configuration;
        this.persister = persister;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final ServiceContainer container = context.getController().getServiceContainer();
        final TrackingServiceTarget serviceTarget = new TrackingServiceTarget(container);
        final DelegatingServiceRegistry serviceRegistry = new DelegatingServiceRegistry(container);
        final Bootstrap.Configuration configuration = this.configuration;
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();
        final ServerControllerImpl serverController = new ServerControllerImpl(new ServerModel(serverEnvironment.getServerName(), configuration.getPortOffset()), container, serverEnvironment);
        final List<AbstractServerModelUpdate<?>> updates;
        try {
            updates = persister.load(serverController);
        } catch (Exception e) {
            throw new StartException(e);
        }
        final BootUpdateContext bootUpdateContext = new BootUpdateContext() {
            public ServiceTarget getServiceTarget() {
                return serviceTarget;
            }

            public ServiceContainer getServiceContainer() {
                throw new UnsupportedOperationException();
            }

            public ServiceRegistry getServiceRegistry() {
                return serviceRegistry;
            }

            public void addDeploymentProcessor(final Phase phase, final DeploymentUnitProcessor processor, final int priority) {
                serverController.registerDeployer(phase, processor, priority);
            }
        };
        log.info("Activating core services");

        // TODO: decide the fate of these

        // Graceful shutdown
        ShutdownHandlerImpl.addService(serviceTarget);

        // Server environment services; todo: drop environment service, fold in path services
        ServerEnvironmentServices.addServices(serverEnvironment, serviceTarget);

        // Socket binding manager
        serviceTarget.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
            new SocketBindingManagerService(configuration.getPortOffset()))
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        

        for (AbstractServerModelUpdate<?> update : updates) {
            update.applyUpdateBootAction(bootUpdateContext);
        }

        this.serverController = serverController;
        bootServices = serviceTarget.getSet();
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        serverController = null;
        final ServiceContainer container = context.getController().getServiceContainer();
        final AtomicInteger count = new AtomicInteger(1);
        final ServiceListener<Object> removeListener = new AbstractServiceListener<Object>() {
            public void listenerAdded(final ServiceController<?> controller) {
                count.incrementAndGet();
            }

            public void serviceRemoved(final ServiceController<?> controller) {
                if (count.decrementAndGet() == 0) {
                    context.complete();
                    Logger.getLogger("org.jboss.as").infof("Stopped JBoss AS in %dms", Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
                }
            }
        };
        context.asynchronous();
        for (ServiceName serviceName : bootServices) {
            final ServiceController<?> controller = container.getService(serviceName);
            if (controller != null) {
                controller.setMode(ServiceController.Mode.REMOVE);
                controller.addListener(removeListener);
            }
        }
        // tick the count down
        removeListener.serviceRemoved(null);
    }

    /** {@inheritDoc} */
    public synchronized ServerController getValue() throws IllegalStateException, IllegalArgumentException {
        return serverController;
    }
}
