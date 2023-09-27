/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health;

import static org.wildfly.extension.health.HealthSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.health.HealthSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.health.HealthSubsystemDefinition.SERVER_HEALTH_PROBES_CAPABILITY;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ServerProbesService implements Service {

    private Consumer<ServerProbesService> consumer;
    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private LocalModelControllerClient modelControllerClient;

    private final Set<ServerProbe> serverProbes = new HashSet<>();

    static void install(OperationContext context) {
        ServiceBuilder<?> sb = context.getServiceTarget().addService(SERVER_HEALTH_PROBES_CAPABILITY.getCapabilityServiceName());

        Consumer<ServerProbesService> consumer = sb.provides(SERVER_HEALTH_PROBES_CAPABILITY.getCapabilityServiceName());
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = sb.requires(context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = sb.requires(context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));

        sb.setInstance(new ServerProbesService(consumer, modelControllerClientFactory, managementExecutor))
                .install();

    }

    private ServerProbesService(Consumer<ServerProbesService> consumer, Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor) {
        this.consumer = consumer;
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // we use a SuperUserClient for the local model controller client so that the server checks can be performed when RBAC is enabled.
        // a doPriviledged block is not needed as these calls are initiated from the management endpoint.
        // The user accessing the management endpoints must be authenticated (if security-enabled is true) but the server checks are not executed on their behalf.
        modelControllerClient = modelControllerClientFactory.get().createSuperUserClient(managementExecutor.get(), true);

        serverProbes.add(new ServerProbes.ServerStateCheck(modelControllerClient));
        serverProbes.add(new ServerProbes.SuspendStateCheck(modelControllerClient));
        serverProbes.add(new ServerProbes.DeploymentsStatusCheck(modelControllerClient));
        serverProbes.add(new ServerProbes.NoBootErrorsCheck(modelControllerClient));

        consumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        serverProbes.clear();
        consumer.accept(null);
        modelControllerClient.close();
    }

    public Set<ServerProbe> getServerProbes() {
        return serverProbes;
    }
}
