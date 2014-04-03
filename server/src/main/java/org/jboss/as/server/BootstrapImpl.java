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

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.jboss.threads.JBossExecutors;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * The bootstrap implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BootstrapImpl implements Bootstrap {

    private static final int MAX_THREADS = ServerEnvironment.getBootstrapMaxThreads();
    private final ShutdownHook shutdownHook;
    private final ServiceContainer container;

    public BootstrapImpl() {
        this.shutdownHook = new ShutdownHook();
        this.container = shutdownHook.register();
    }

    @Override
    public AsyncFuture<ServiceContainer> bootstrap(final Configuration configuration, final List<ServiceActivator> extraServices) {

        try {
            final Object value = ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"), "MaxFileDescriptorCount");
            final long fdCount = Long.valueOf(value.toString()).longValue();
            if (fdCount < 4096L) {
                ServerLogger.FD_LIMIT_LOGGER.fdTooLow(fdCount);
            }
        } catch (Throwable ignored) {}

        assert configuration != null : "configuration is null";

        // AS7-6381 set this property so we can get it out of the launch scripts
        String resolverWarning = WildFlySecurityManager.getPropertyPrivileged("org.jboss.resolver.warning", null);
        if (resolverWarning == null) {
            WildFlySecurityManager.setPropertyPrivileged("org.jboss.resolver.warning", "true");
        }

        final ModuleLoader moduleLoader = configuration.getModuleLoader();
        final Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = configuration.getConfigurationPersisterFactory();
        assert configurationPersisterFactory != null : "configurationPersisterFactory is null";

        try {
            Module.registerURLStreamHandlerFactoryModule(moduleLoader.loadModule(ModuleIdentifier.create("org.jboss.vfs")));
        } catch (ModuleLoadException e) {
            throw ServerMessages.MESSAGES.vfsNotAvailable();
        }
        final FutureServiceContainer future = new FutureServiceContainer(container);
        final ServiceTarget tracker = container.subTarget();
        final ControlledProcessState processState = new ControlledProcessState(true);
        shutdownHook.setControlledProcessState(processState);
        ControlledProcessStateService.addService(tracker, processState);
        final Service<?> applicationServerService = new ApplicationServerService(extraServices, configuration, processState);
        tracker.addService(Services.JBOSS_AS, applicationServerService)
            .install();
        final ServiceController<?> rootService = container.getRequiredService(Services.JBOSS_AS);
        rootService.addListener(new AbstractServiceListener<Object>() {
            @Override
            public void transition(final ServiceController<?> controller, final ServiceController.Transition transition) {
                switch (transition) {
                    case STARTING_to_UP: {
                        controller.removeListener(this);
                        final ServiceController<?> controllerServiceController = controller.getServiceContainer().getRequiredService(Services.JBOSS_SERVER_CONTROLLER);
                        controllerServiceController.addListener(new AbstractServiceListener<Object>() {
                            public void transition(final ServiceController<?> controller, final ServiceController.Transition transition) {
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
                                        future.failed(ServerMessages.MESSAGES.serverControllerServiceRemoved());
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
                        future.failed(ServerMessages.MESSAGES.rootServiceRemoved());
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
            return (AsyncFuture<ServiceContainer>) controller.getValue();
        } catch (Exception ex) {
            throw ServerMessages.MESSAGES.cannotStartServer(ex);
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

    private static class ShutdownHook extends Thread {
        private boolean down;
        private ControlledProcessState processState;
        private ServiceContainer container;

        private ServiceContainer register() {

            Runtime.getRuntime().addShutdownHook(this);
            synchronized (this) {
                if (!down) {
                    container = ServiceContainer.Factory.create("jboss-as", MAX_THREADS, 30, TimeUnit.SECONDS, false);
                    return container;
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        private synchronized void setControlledProcessState(final ControlledProcessState ps) {
            this.processState = ps;
        }

        @Override
        public void run() {
            final ServiceContainer sc;
            final ControlledProcessState ps;
            synchronized (this) {
                down = true;
                sc = container;
                ps = processState;
            }
            try {
                if (ps != null) {
                    ps.setStopping();
                }
            } finally {
                if (sc != null) {
                    final CountDownLatch latch = new CountDownLatch(1);
                    sc.addTerminateListener(new ServiceContainer.TerminateListener() {
                        @Override
                        public void handleTermination(Info info) {
                            latch.countDown();
                        }
                    });
                    sc.shutdown();
                    // wait for all services to finish.
                    for (;;) {
                        try {
                            latch.await();
                            break;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }
}
