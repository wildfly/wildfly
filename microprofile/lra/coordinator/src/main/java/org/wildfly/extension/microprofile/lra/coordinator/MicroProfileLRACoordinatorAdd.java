/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.extension.microprofile.lra.coordinator._private.MicroProfileLRACoordinatorLogger;
import org.wildfly.extension.microprofile.lra.coordinator.service.LRACoordinatorService;
import org.wildfly.extension.microprofile.lra.coordinator.service.LRARecoveryService;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.wildfly.extension.microprofile.lra.coordinator.MicroProfileLRACoordinatorSubsystemDefinition.ATTRIBUTES;

class MicroProfileLRACoordinatorAdd extends AbstractBoottimeAddStepHandler {

    MicroProfileLRACoordinatorAdd() {
        super(Arrays.asList(ATTRIBUTES));
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        registerRecoveryService(context);
        registerCoordinatorService(context, model);

        MicroProfileLRACoordinatorLogger.LOGGER.activatingSubsystem();
    }

    private void registerCoordinatorService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        CapabilityServiceBuilder builder = context.getCapabilityServiceTarget()
            .addCapability(MicroProfileLRACoordinatorSubsystemDefinition.LRA_COORDINATOR_CAPABILITY);

        builder.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        String serverModelValue = MicroProfileLRACoordinatorSubsystemDefinition.SERVER.resolveModelAttribute(context, model).asString();
        String hostModelValue = MicroProfileLRACoordinatorSubsystemDefinition.HOST.resolveModelAttribute(context, model).asString();
        Supplier<Host> hostSupplier = builder.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverModelValue, hostModelValue);

        final LRACoordinatorService lraCoordinatorService = new LRACoordinatorService(hostSupplier);

        builder.requiresCapability(MicroProfileLRACoordinatorSubsystemDefinition.LRA_RECOVERY_SERVICE_CAPABILITY_NAME, null);

        builder.setInstance(lraCoordinatorService);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private void registerRecoveryService(final OperationContext context) {
        CapabilityServiceBuilder builder = context.getCapabilityServiceTarget().addCapability(
            MicroProfileLRACoordinatorSubsystemDefinition.LRA_RECOVERY_SERVICE_CAPABILITY);
        builder.provides(MicroProfileLRACoordinatorSubsystemDefinition.LRA_RECOVERY_SERVICE_CAPABILITY);
        // JTA is required to be loaded before the LRA recovery setup
        builder.requiresCapability(MicroProfileLRACoordinatorSubsystemDefinition.REF_JTA_RECOVERY_CAPABILITY, XAResourceRecoveryRegistry.class);
        Supplier<ExecutorService> executorSupplier = Services.requireServerExecutor(builder);
        final LRARecoveryService lraRecoveryService = new LRARecoveryService(executorSupplier);
        builder.setInstance(lraRecoveryService);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}