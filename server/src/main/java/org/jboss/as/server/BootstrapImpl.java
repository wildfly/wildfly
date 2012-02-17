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
import java.util.concurrent.TimeUnit;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossExecutors;

/**
 * The bootstrap implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BootstrapImpl implements Bootstrap {
    private static final int MAX_THREADS = ServerEnvironment.getBootstrapMaxThreads();
    private final ServiceContainer container = ServiceContainer.Factory.create("jboss-as", MAX_THREADS, 30, TimeUnit.SECONDS);

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
        final Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = configuration.getConfigurationPersisterFactory();
        if (moduleLoader == null) {
            throw new IllegalArgumentException("moduleLoader is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (configurationPersisterFactory == null) {
            throw new IllegalArgumentException("configurationPersisterFactory is null");
        }
        try {
            Module.registerURLStreamHandlerFactoryModule(moduleLoader.loadModule(ModuleIdentifier.create("org.jboss.vfs")));
        } catch (ModuleLoadException e) {
            throw new IllegalArgumentException("VFS is not available from the configured module loader");
        }
        final FutureServiceContainer future = new FutureServiceContainer(container);
        final ServiceTarget tracker = container.subTarget();
        final Service<?> applicationServerService = new ApplicationServerService(extraServices, configuration);
        tracker.addService(Services.JBOSS_AS, applicationServerService)
            .install();
        final ServiceController<?> rootService = container.getRequiredService(Services.JBOSS_AS);
        rootService.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                switch (transition) {
                    case STARTING_to_UP: {
                        controller.removeListener(this);
                        final ServiceController<?> controllerServiceController = controller.getServiceContainer().getRequiredService(Services.JBOSS_SERVER_CONTROLLER);
                        controllerServiceController.addListener(new AbstractServiceListener<Object>() {
                            public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                                switch (transition) {
                                    case STARTING_to_UP: {
                                        future.done();
                                        controller.removeListener(this);
                                        break;
                                    }
                                    case STARTING_to_START_FAILED: {
                                        future.failed(controller.getStartException());
                                        controller.removeListener(this);
                                        break;
                                    }
                                    case REMOVING_to_REMOVED: {
                                        future.failed(new ServiceNotFoundException("Server controller service was removed"));
                                        controller.removeListener(this);
                                        break;
                                    }
                                }
                            }
                        });
                        break;
                    }
                    case STARTING_to_START_FAILED: {
                        controller.removeListener(this);
                        future.failed(controller.getStartException());
                        break;
                    }
                    case REMOVING_to_REMOVED: {
                        controller.removeListener(this);
                        future.failed(new ServiceNotFoundException("Root service was removed"));
                        break;
                    }
                }
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

    static class FutureServiceContainer extends AsyncFutureTask<ServiceContainer> {
        private final ServiceContainer container;

        FutureServiceContainer(final ServiceContainer container) {
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

        void done() {
            setResult(container);
        }

        void failed(Throwable t) {
            setFailed(t);
        }
    }
}
