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

package org.jboss.as.host.controller;

import static org.jboss.as.server.ServerLogger.AS_ROOT_LOGGER;
import static org.jboss.as.server.ServerLogger.CONFIG_LOGGER;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.security.AccessController;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.server.BootstrapListener;
import org.jboss.as.server.FutureServiceContainer;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.JBossThreadFactory;

/**
 * The root service for a HostController process.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HostControllerService implements Service<AsyncFuture<ServiceContainer>> {

    public static final ServiceName HC_SERVICE_NAME = ServiceName.JBOSS.append("host", "controller");
    static final ServiceName HC_EXECUTOR_SERVICE_NAME = HC_SERVICE_NAME.append("executor");
    static final int DEFAULT_POOL_SIZE = 20;

    private final HostControllerEnvironment environment;
    private final HostRunningModeControl runningModeControl;
    private final ControlledProcessState processState;
    private final byte[] authCode;
    private volatile FutureServiceContainer futureContainer;
    private volatile long startTime;

    HostControllerService(final HostControllerEnvironment environment, final HostRunningModeControl runningModeControl,
                          final byte[] authCode, final ControlledProcessState processState) {
        this.environment = environment;
        this.runningModeControl = runningModeControl;
        this.authCode = authCode;
        this.processState = processState;
        this.startTime = Module.getStartTime();
    }

    @Override
    public void start(StartContext context) throws StartException {

        processState.setStarting();

        String prettyVersion = environment.getProductConfig().getPrettyVersionString();
        AS_ROOT_LOGGER.serverStarting(prettyVersion);
        if (CONFIG_LOGGER.isDebugEnabled()) {
            final Properties properties = System.getProperties();
            final StringBuilder b = new StringBuilder(8192);
            b.append("Configured system properties:");
            for (String property : new TreeSet<String>(properties.stringPropertyNames())) {
                b.append("\n\t").append(property).append(" = ").append(properties.getProperty(property, "<undefined>"));
            }
            CONFIG_LOGGER.debug(b);
            CONFIG_LOGGER.debugf("VM Arguments: %s", getVMArguments());
            if (CONFIG_LOGGER.isTraceEnabled()) {
                b.setLength(0);
                final Map<String, String> env = System.getenv();
                b.append("Configured system environment:");
                for (String key : new TreeSet<String>(env.keySet())) {
                    b.append("\n\t").append(key).append(" = ").append(env.get(key));
                }
                CONFIG_LOGGER.trace(b);
            }
        }
        final ServiceTarget serviceTarget = context.getChildTarget();
        final ServiceController<?> myController = context.getController();
        final ServiceContainer serviceContainer = myController.getServiceContainer();
        futureContainer = new FutureServiceContainer();

        long startTime = this.startTime;
        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        } else {
            this.startTime = -1;
        }

        final BootstrapListener bootstrapListener = new BootstrapListener(serviceContainer, startTime, serviceTarget, futureContainer,  prettyVersion + " (Host Controller)");
        serviceTarget.addListener(ServiceListener.Inheritance.ALL, bootstrapListener);
        myController.addListener(bootstrapListener);

        // The first default services are registered before the bootstrap operations are executed.

        // Install the process controller client
        final ProcessControllerConnectionService processControllerClient = new ProcessControllerConnectionService(environment, authCode);
        serviceTarget.addService(ProcessControllerConnectionService.SERVICE_NAME, processControllerClient).install();

        // Executor Services
        final HostControllerExecutorService executorService = new HostControllerExecutorService();
        serviceTarget.addService(HC_EXECUTOR_SERVICE_NAME, executorService)
                .install();

        // Install required path services. (Only install those identified as required)
        HostPathManagerService hostPathManagerService = new HostPathManagerService();
        HostPathManagerService.addService(serviceTarget, hostPathManagerService, environment);

        DomainModelControllerService.addService(serviceTarget, environment, runningModeControl, processState, bootstrapListener, hostPathManagerService);
    }

    @Override
    public void stop(StopContext context) {
        String prettyVersion = environment.getProductConfig().getPrettyVersionString();
        processState.setStopping();
        AS_ROOT_LOGGER.serverStopped(prettyVersion, Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
    }

    @Override
    public AsyncFuture<ServiceContainer> getValue() throws IllegalStateException, IllegalArgumentException {
        return futureContainer;
    }

    public HostControllerEnvironment getEnvironment() {
        return environment;
    }

    private String getVMArguments() {
        final StringBuilder result = new StringBuilder(1024);
        final RuntimeMXBean rmBean = ManagementFactory.getRuntimeMXBean();
        final List<String> inputArguments = rmBean.getInputArguments();
        for (String arg : inputArguments) {
            result.append(arg).append(" ");
        }
        return result.toString();
    }

    static final class HostControllerExecutorService implements Service<ExecutorService> {
        final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("Host Controller Service Threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        private ExecutorService executorService;

        @Override
        public synchronized void start(final StartContext context) throws StartException {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    5L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    threadFactory);
        }

        @Override
        public synchronized void stop(final StopContext context) {
            context.asynchronous();
            Thread executorShutdown = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        executorService.shutdown();
                    } finally {
                        executorService = null;
                        context.complete();
                    }
                }
            }, "HostController ExecutorService Shutdown Thread");
            executorShutdown.start();
        }

        @Override
        public synchronized ExecutorService getValue() throws IllegalStateException {
            return executorService;
        }
    }
}
