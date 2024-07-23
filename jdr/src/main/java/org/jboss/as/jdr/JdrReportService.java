/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import static org.jboss.as.jdr.JdrReportSubsystemDefinition.JDR_CAPABILITY;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.server.ServerEnvironment;
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
        Supplier<ModelControllerClientFactory> mcfSupplier = builder.requires(ModelControllerClientFactory.SERVICE_DESCRIPTOR);
        Supplier<Executor> executorSupplier = builder.requires(Capabilities.MANAGEMENT_EXECUTOR);
        Supplier<ServerEnvironment> seSupplier = builder.requires(ServerEnvironment.SERVICE_DESCRIPTOR);
        builder.setInstance(new JdrReportService(subsystemConsumer, mscConsumer, mcfSupplier, executorSupplier, seSupplier))
                .setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    /** Consumer we use to pass a ref back to our subsystem code for its use in OSHs */
    private final Consumer<JdrReportCollector> subsystemConsumer;
    /** Consumer we use to pass a ref to MSC for its use in service-based injection */
    private final Consumer<JdrReportCollector> mscConsumer;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;
    private final Supplier<ModelControllerClientFactory> clientFactorySupplier;
    private final Supplier<Executor> executorSupplier;

    private JdrReportService(final Consumer<JdrReportCollector> subsystemConsumer,
                             final Consumer<JdrReportCollector> consumer,
                             final Supplier<ModelControllerClientFactory> mcfSupplier,
                             final Supplier<Executor> executorSupplier,
                             final Supplier<ServerEnvironment> seSupplier) {
        this.subsystemConsumer = subsystemConsumer;
        this.mscConsumer = consumer;
        this.serverEnvironmentSupplier = seSupplier;
        this.clientFactorySupplier = mcfSupplier;
        this.executorSupplier = executorSupplier;
    }

    /**
     * Collect a JDR report.
     */
    public JdrReport collect() throws OperationFailedException {

        try (LocalModelControllerClient client = clientFactorySupplier.get().createSuperUserClient(executorSupplier.get())) {
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
