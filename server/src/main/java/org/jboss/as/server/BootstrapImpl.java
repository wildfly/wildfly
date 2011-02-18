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

import java.util.List;

import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossExecutors;

/**
 * The bootstrap implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BootstrapImpl implements Bootstrap {
    private final ServiceContainer container = ServiceContainer.Factory.create("jboss-as");

    @Override
    public AsyncFuture<ServiceContainer> bootstrap(final Configuration configuration, final List<ServiceActivator> extraServices) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration is null");
        }
        final ModuleLoader moduleLoader = configuration.getModuleLoader();
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();
        if (serverEnvironment == null) {
            throw new IllegalArgumentException("serverEnvironment is null");
        }
        final String name = serverEnvironment.getServerName();
        final NewConfigurationPersister configurationPersister = configuration.getConfigurationPersister();
        if (moduleLoader == null) {
            throw new IllegalArgumentException("moduleLoader is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (configurationPersister == null) {
            throw new IllegalArgumentException("configurationPersister is null");
        }
        final FutureServiceContainer future = new FutureServiceContainer(container);
        final ServiceTarget tracker = container.subTarget();
        final Service<?> applicationServerService = new ApplicationServerService(extraServices, configuration);
        tracker.addService(Services.JBOSS_AS, applicationServerService)
            .install();
        final ServiceController<?> rootService = container.getRequiredService(Services.JBOSS_AS);
        rootService.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void serviceStarted(final ServiceController<?> controller) {
                controller.removeListener(this);
                final ServiceController<?> controllerServiceController = controller.getServiceContainer().getRequiredService(Services.JBOSS_SERVER_CONTROLLER);
                controllerServiceController.addListener(new AbstractServiceListener<Object>() {
                    @Override
                    public void serviceStarted(final ServiceController<?> controller) {
                        future.done((ServerController) controller.getValue());
                        controller.removeListener(this);
                    }

                    @Override
                    public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
                        future.failed(reason);
                        controller.removeListener(this);
                    }

                    @Override
                    public void serviceRemoved(final ServiceController<?> controller) {
                        future.failed(new ServiceNotFoundException("Server controller service was removed"));
                        controller.removeListener(this);
                    }
                });
            }

            @Override
            public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
                controller.removeListener(this);
                future.failed(reason);
            }

            @Override
            public void serviceRemoved(final ServiceController<?> controller) {
                controller.removeListener(this);
                future.failed(new ServiceNotFoundException("Root service was removed"));
            }
        });
        return future;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AsyncFuture<ServiceContainer> startup(Configuration configuration, List<ServiceActivator> extraServices) {
        try {
            ServiceContainer container = bootstrap(configuration, extraServices).get();
            ServiceController<?> controller = container.getRequiredService(Services.JBOSS_AS);
            AsyncFuture<ServiceContainer> startupFuture = (AsyncFuture<ServiceContainer>) controller.getValue();
            return startupFuture;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot start server", ex);
        }
    }

    private static class FutureServiceContainer extends AsyncFutureTask<ServiceContainer> {
        private final ServiceContainer container;

        public FutureServiceContainer(final ServiceContainer container) {
            super(JBossExecutors.directExecutor());
            this.container = container;
        }

        @Override
        public void asyncCancel(final boolean interruptionDesired) {
            container.shutdown();
            container.addTerminateListener(new ServiceContainer.TerminateListener() {
                @Override
                public void handleTermination(final Info info) {
                    setCancelled();
                }
            });
        }

        void done(ServerController controller) {
            setResult(container);
        }

        void failed(Throwable t) {
            setFailed(t);
        }
    }
}
