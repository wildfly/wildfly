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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.version.Version;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
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

    public AsyncFuture<ServerController> start(final Configuration configuration, final List<ServiceActivator> extraServices) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration is null");
        }
        final ModuleLoader moduleLoader = configuration.getModuleLoader();
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();
        final String name = serverEnvironment.getServerName();
        final ServerConfigurationPersister configurationPersister = configuration.getConfigurationPersister();
        if (moduleLoader == null) {
            throw new IllegalArgumentException("moduleLoader is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (serverEnvironment == null) {
            throw new IllegalArgumentException("serverEnvironment is null");
        }
        if (configurationPersister == null) {
            throw new IllegalArgumentException("configurationPersister is null");
        }
        final ServiceContainer container = ServiceContainer.Factory.create("jboss-as-server");
        final StartTask future = new StartTask(container);
        final ServiceTarget tracker = container.subTarget();
        final Service<?> applicationServerService = new ApplicationServerService(extraServices, configuration);
        container.addListener(new BootstrapListener(future, container, configuration.getStartTime()));
        tracker.addService(Services.JBOSS_AS, applicationServerService)
            .install();
        return future;
    }

    private static class StartTask extends AsyncFutureTask<ServerController> {
        private final ServiceContainer container;

        public StartTask(final ServiceContainer container) {
            super(JBossExecutors.directExecutor());
            this.container = container;
        }

        public void asyncCancel(final boolean interruptionDesired) {
            container.shutdown();
            container.addTerminateListener(new ServiceContainer.TerminateListener() {
                public void handleTermination(final Info info) {
                    setCancelled();
                }
            });
        }

        void done(ServerController controller) {
            setResult(controller);
        }

        void failed(Throwable t) {
            setFailed(t);
        }

        void failed(final int failed) {
            setFailed(new Exception(String.format("Server failed to start (%d services failed)", Integer.valueOf(failed))));
        }
    }

    private static class BootstrapListener extends AbstractServiceListener<Object> {

        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger outstanding = new AtomicInteger();
        private final AtomicBoolean done = new AtomicBoolean();
        private final AtomicInteger missingDeps = new AtomicInteger();
        private final EnumMap<ServiceController.Mode, AtomicInteger> map;
        private final StartTask future;
        private final ServiceContainer serviceContainer;
        private final long startTime;
        private final Set<ServiceName> missingDepsSet = Collections.synchronizedSet(new TreeSet<ServiceName>());
        private volatile boolean cancelLikely;

        public BootstrapListener(final StartTask future, final ServiceContainer serviceContainer, final long startTime) {
            this.future = future;
            this.serviceContainer = serviceContainer;
            this.startTime = startTime;
            final EnumMap<ServiceController.Mode, AtomicInteger> map = new EnumMap<ServiceController.Mode, AtomicInteger>(ServiceController.Mode.class);
            for (ServiceController.Mode mode : ServiceController.Mode.values()) {
                map.put(mode, new AtomicInteger());
            }
            this.map = map;
        }

        public void listenerAdded(final ServiceController<?> controller) {
            final ServiceController.Mode mode = controller.getMode();
            if (mode == ServiceController.Mode.ACTIVE) {
                outstanding.incrementAndGet();
            } else {
                controller.removeListener(this);
            }
            map.get(mode).incrementAndGet();
        }

        public void serviceStarted(final ServiceController<?> controller) {
            started.incrementAndGet();
            controller.removeListener(this);
            tick(future.container);
        }

        public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
            failed.incrementAndGet();
            controller.removeListener(this);
            tick(controller.getServiceContainer());
        }

        public void dependencyFailed(final ServiceController<? extends Object> controller) {
            controller.removeListener(this);
            tick(controller.getServiceContainer());
        }

        public void dependencyUninstalled(final ServiceController<? extends Object> controller) {
            missingDeps.incrementAndGet();
            missingDepsSet.add(controller.getName());
            check();
        }

        public void dependencyInstalled(final ServiceController<? extends Object> controller) {
            missingDeps.decrementAndGet();
            missingDepsSet.remove(controller.getName());
            check();
        }

        public void serviceRemoved(final ServiceController<?> controller) {
            cancelLikely = true;
            controller.removeListener(this);
            tick(controller.getServiceContainer());
        }

        private void check() {
            int outstanding = this.outstanding.get();
            if (outstanding == missingDeps.get()) {
                finish(serviceContainer, outstanding);
            }
        }

        private void tick(final ServiceContainer container) {
            int outstanding = this.outstanding.decrementAndGet();
            if (outstanding != missingDeps.get()) {
                return;
            }
            finish(container, outstanding);
        }

        private void finish(final ServiceContainer container, final int outstanding) {
            if (done.getAndSet(true)) {
                return;
            }
            container.removeListener(this);
            if (cancelLikely) {
                return;
            }
            final int failed = this.failed.get() + outstanding;
            final ServiceController<?> serverControllerService;
            try {
                serverControllerService = container.getRequiredService(Services.JBOSS_SERVER_CONTROLLER);
                future.done((ServerController) serverControllerService.getValue());
            } catch (ServiceNotFoundException e) {
                future.failed(e);
            }
            final long elapsedTime = Math.max(System.currentTimeMillis() - startTime, 0L);
            final Logger log = Logger.getLogger("org.jboss.as");
            final int started = this.started.get();
            final int active = map.get(ServiceController.Mode.ACTIVE).get();
            final int passive = map.get(ServiceController.Mode.PASSIVE).get();
            final int onDemand = map.get(ServiceController.Mode.ON_DEMAND).get();
            final int never = map.get(ServiceController.Mode.NEVER).get();
            serviceContainer.removeListener(this);
            if (failed == 0) {
                log.infof("JBoss AS %s \"%s\" started in %dms - Started %d of %d services (%d services are passive or on-demand)", Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Long.valueOf(elapsedTime), Integer.valueOf(started), Integer.valueOf(active + passive + onDemand + never), Integer.valueOf(onDemand + passive));
            } else {
                final StringBuilder b = new StringBuilder();
                b.append(String.format("JBoss AS %s \"%s\" started (with errors) in %dms - Started %d of %d services (%d services failed or missing dependencies, %d services are passive or on-demand)", Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Long.valueOf(elapsedTime), Integer.valueOf(started), Integer.valueOf(active + passive + onDemand + never), Integer.valueOf(failed), Integer.valueOf(onDemand + passive)));
                final Set<ServiceName> set = missingDepsSet;
                final Iterator<ServiceName> i = set.iterator();
                if (i.hasNext()) {
                    b.append("\n    Services missing dependencies:");
                    do {
                        b.append("\n        ").append(i.next());
                    } while (i.hasNext());
                }
                log.error(b);
            }
        }
    }
}
