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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.server.deployment.impl.ServerDeploymentRepositoryImpl;
import org.jboss.as.server.mgmt.ShutdownHandler;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.version.Version;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ApplicationServerService implements Service<Void> {

    private static final Logger log = Logger.getLogger("org.jboss.as");
    private static final Logger configLog = Logger.getLogger("org.jboss.as.config");
    private final List<ServiceActivator> extraServices;
    private final Bootstrap.Configuration configuration;
    private volatile long startTime;

    ApplicationServerService(final List<ServiceActivator> extraServices, final Bootstrap.Configuration configuration) {
        this.extraServices = extraServices;
        this.configuration = configuration;
        startTime = configuration.getStartTime();
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final Bootstrap.Configuration configuration = this.configuration;
        final ServerEnvironment serverEnvironment = configuration.getServerEnvironment();

        // Install the environment before doing anything
        serverEnvironment.install();

        log.infof("JBoss AS %s \"%s\" starting", Version.AS_VERSION, Version.AS_RELEASE_CODENAME);
        if (configLog.isDebugEnabled()) {
            final Properties properties = System.getProperties();
            final StringBuilder b = new StringBuilder(8192);
            b.append("Configured system properties:");
            for (String property : new TreeSet<String>(properties.stringPropertyNames())) {
                b.append("\n\t").append(property).append(" = ").append(properties.getProperty(property, "<undefined>"));
            }
            configLog.debug(b);
            if (configLog.isTraceEnabled()) {
                b.setLength(0);
                final Map<String,String> env = System.getenv();
                b.append("Configured system environment:");
                for (String key : new TreeSet<String>(env.keySet())) {
                    b.append("\n\t").append(key).append(" = ").append(env.get(key));
                }
                configLog.trace(b);
            }
        }
        final ServiceTarget serviceTarget = context.getChildTarget();
        final ServiceController<?> myController = context.getController();
        final ServiceContainer container = myController.getServiceContainer();
        long startTime = this.startTime;
        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        } else {
            this.startTime = -1;
        }
        final BootstrapListener bootstrapListener = new BootstrapListener(startTime, serviceTarget);
        serviceTarget.addListener(bootstrapListener);
        myController.addListener(bootstrapListener);
        ServerDeploymentRepositoryImpl.addService(serviceTarget, serverEnvironment.getServerDeployDir(), serverEnvironment.getServerSystemDeployDir());
        ServiceModuleLoader.addService(serviceTarget, configuration);
        ExternalModuleService.addService(serviceTarget);
        ServerControllerService.addService(serviceTarget, configuration);
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

        if (log.isDebugEnabled()) {
            final long nanos = context.getElapsedTime();
            log.debugf("JBoss AS root service started in %d.%06d ms", Long.valueOf(nanos / 1000000L), Long.valueOf(nanos % 1000000L));
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        log.infof("JBoss AS %s \"%s\" stopped in %dms", Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    private static class BootstrapListener extends AbstractServiceListener<Object> {

        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger outstanding = new AtomicInteger();
        private final AtomicBoolean done = new AtomicBoolean();
        private final AtomicInteger missingDeps = new AtomicInteger();
        private final EnumMap<ServiceController.Mode, AtomicInteger> map;
        private final ServiceTarget serviceTarget;
        private final long startTime;
        private final Set<ServiceName> missingDepsSet = Collections.synchronizedSet(new TreeSet<ServiceName>());
        private volatile boolean cancelLikely;

        public BootstrapListener(final long startTime, final ServiceTarget serviceTarget) {
            this.startTime = startTime;
            this.serviceTarget = serviceTarget;
            final EnumMap<ServiceController.Mode, AtomicInteger> map = new EnumMap<ServiceController.Mode, AtomicInteger>(ServiceController.Mode.class);
            for (ServiceController.Mode mode : ServiceController.Mode.values()) {
                map.put(mode, new AtomicInteger());
            }
            this.map = map;
        }

        @Override
        public void listenerAdded(final ServiceController<?> controller) {
            final ServiceController.Mode mode = controller.getMode();
            if (mode == ServiceController.Mode.ACTIVE) {
                outstanding.incrementAndGet();
            } else {
                controller.removeListener(this);
            }
            map.get(mode).incrementAndGet();
        }

        @Override
        public void serviceStarted(final ServiceController<?> controller) {
            started.incrementAndGet();
            controller.removeListener(this);
            tick();
        }

        @Override
        public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
            failed.incrementAndGet();
            controller.removeListener(this);
            tick();
        }

        @Override
        public void dependencyFailed(final ServiceController<? extends Object> controller) {
            controller.removeListener(this);
            tick();
        }

        @Override
        public void dependencyUninstalled(final ServiceController<? extends Object> controller) {
            missingDeps.incrementAndGet();
            missingDepsSet.add(controller.getName());
            check();
        }

        @Override
        public void dependencyInstalled(final ServiceController<? extends Object> controller) {
            missingDeps.decrementAndGet();
            missingDepsSet.remove(controller.getName());
            check();
        }

        @Override
        public void serviceRemoved(final ServiceController<?> controller) {
            cancelLikely = true;
            controller.removeListener(this);
            tick();
        }

        private void check() {
            int outstanding = this.outstanding.get();
            if (outstanding == missingDeps.get()) {
                finish(outstanding);
            }
        }

        private void tick() {
            int outstanding = this.outstanding.decrementAndGet();
            if (outstanding != missingDeps.get()) {
                return;
            }
            finish(outstanding);
        }

        private void finish(final int outstanding) {
            if (done.getAndSet(true)) {
                return;
            }
            serviceTarget.removeListener(this);
            if (cancelLikely) {
                return;
            }
            final int failed = this.failed.get() + outstanding;
            final long elapsedTime = Math.max(System.currentTimeMillis() - startTime, 0L);
            final Logger log = Logger.getLogger("org.jboss.as");
            final int started = this.started.get();
            final int active = map.get(ServiceController.Mode.ACTIVE).get();
            final int passive = map.get(ServiceController.Mode.PASSIVE).get();
            final int onDemand = map.get(ServiceController.Mode.ON_DEMAND).get();
            final int never = map.get(ServiceController.Mode.NEVER).get();
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
