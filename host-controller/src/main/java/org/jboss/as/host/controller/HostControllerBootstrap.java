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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Bootstrap of the HostController process.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HostControllerBootstrap {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller");
    static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("host", "controller");
    static final int DEFAULT_POOL_SIZE = 20;
    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create("host-controller");
    private final HostControllerEnvironment environment;
    private final byte[] authCode;

    public HostControllerBootstrap(final HostControllerEnvironment environment, final byte[] authCode) {
        this.environment = environment;
        this.authCode = authCode;
    }

    /**
     * Start the host controller services.
     *
     * @throws Exception
     */
    public void start() throws Exception {

        // TODO BootstrapListener

        // The first default services are registered before the bootstrap operations are executed.
        final ServiceTarget serviceTarget = serviceContainer;

        // Install the process controller client
        final ProcessControllerConnectionService processControllerClient = new ProcessControllerConnectionService(environment, authCode);
        serviceTarget.addService(ProcessControllerConnectionService.SERVICE_NAME, processControllerClient).install();

        // Thread Factory and Executor Services
        final ServiceName threadFactoryServiceName = SERVICE_NAME_BASE.append("thread-factory");
        final ServiceName executorServiceName = SERVICE_NAME_BASE.append("executor");

        serviceTarget.addService(threadFactoryServiceName, new ThreadFactoryService()).install();
        final HostControllerExecutorService executorService = new HostControllerExecutorService();
        serviceTarget.addService(executorServiceName, executorService)
                .addDependency(threadFactoryServiceName, ThreadFactory.class, executorService.threadFactoryValue)
                .install();

        // Install required path services. (Only install those identified as required)
        AbsolutePathService.addService(HostControllerEnvironment.HOME_DIR, environment.getHomeDir().getAbsolutePath(), serviceTarget);
        AbsolutePathService.addService(HostControllerEnvironment.DOMAIN_CONFIG_DIR, environment.getDomainConfigurationDir().getAbsolutePath(), serviceTarget);

        DomainModelControllerService.addService(serviceTarget, environment, new ControlledProcessState(false));
    }

    static final class HostControllerExecutorService implements Service<Executor> {
        final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
        private ScheduledExecutorService executorService;

        @Override
        public synchronized void start(final StartContext context) throws StartException {
            executorService = Executors.newScheduledThreadPool(DEFAULT_POOL_SIZE, threadFactoryValue.getValue());
        }

        @Override
        public synchronized void stop(final StopContext context) {
            executorService.shutdown();
        }

        @Override
        public synchronized ScheduledExecutorService getValue() throws IllegalStateException {
            return executorService;
        }
    }

}
