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

package org.jboss.as.jdr;

import static org.jboss.as.jdr.JdrReportSubsystemDefinition.EXECUTOR_CAPABILITY;
import static org.jboss.as.jdr.JdrReportSubsystemDefinition.JDR_CAPABILITY;
import static org.jboss.as.jdr.JdrReportSubsystemDefinition.MCF_CAPABILITY;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides a {@link JdrReportCollector}.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 * @author Jesse Jaggars
 */
class JdrReportService implements JdrReportCollector, Service {

    /**
     * Install the JdrReportService.
     *
     * @param target MSC target to use to install
     * @param subsystemConsumer consumer to provide a ref to the started service to the rest of the subsystem for its use in OSHs.
     */
    static void addService(final CapabilityServiceTarget target, final Consumer<JdrReportCollector> subsystemConsumer) {

        CapabilityServiceBuilder<?> builder = target.addCapability(JDR_CAPABILITY);
        Consumer<JdrReportCollector> mscConsumer = builder.provides(JDR_CAPABILITY);
        Supplier<ModelControllerClientFactory> mcfSupplier =
                builder.requiresCapability(MCF_CAPABILITY, ModelControllerClientFactory.class);
        Supplier<ExecutorService> esSupplier =
                builder.requiresCapability(EXECUTOR_CAPABILITY, ExecutorService.class);
        Supplier<ServerEnvironment> seSupplier = builder.requires(ServerEnvironmentService.SERVICE_NAME);
        builder.setInstance(new JdrReportService(subsystemConsumer, mscConsumer, mcfSupplier, esSupplier, seSupplier))
                .setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    /** Consumer we use to pass a ref back to our subsystem code for its use in OSHs */
    private final Consumer<JdrReportCollector> subsystemConsumer;
    /** Consumer we use to pass a ref to MSC for its use in service-based injection */
    private final Consumer<JdrReportCollector> mscConsumer;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;
    private final Supplier<ModelControllerClientFactory> clientFactorySupplier;
    private final Supplier<ExecutorService> executorServiceSupplier;

    private JdrReportService(final Consumer<JdrReportCollector> subsystemConsumer,
                             final Consumer<JdrReportCollector> consumer,
                             final Supplier<ModelControllerClientFactory> mcfSupplier,
                             final Supplier<ExecutorService> executorServiceSupplier,
                             final Supplier<ServerEnvironment> seSupplier) {
        this.subsystemConsumer = subsystemConsumer;
        this.mscConsumer = consumer;
        this.serverEnvironmentSupplier = seSupplier;
        this.clientFactorySupplier = mcfSupplier;
        this.executorServiceSupplier = executorServiceSupplier;
    }

    /**
     * Collect a JDR report.
     */
    public JdrReport collect() throws OperationFailedException {

        try (LocalModelControllerClient client = clientFactorySupplier.get().createSuperUserClient(executorServiceSupplier.get())) {
            JdrRunner runner = new JdrRunner(true);
            ServerEnvironment serverEnvironment = serverEnvironmentSupplier.get();
            runner.setJbossHomeDir(serverEnvironment.getHomeDir().getAbsolutePath());
            runner.setReportLocationDir(serverEnvironment.getServerTempDir().getAbsolutePath());
            runner.setControllerClient(client);
            runner.setHostControllerName(serverEnvironment.getHostControllerName());
            runner.setServerName(serverEnvironment.getServerName());
            return runner.collect();
        }
    }

    public synchronized void start(StartContext context) {
        mscConsumer.accept(this);
        subsystemConsumer.accept(this);
    }

    public synchronized void stop(StopContext context) {
        mscConsumer.accept(null);
        subsystemConsumer.accept(null);
    }
}
