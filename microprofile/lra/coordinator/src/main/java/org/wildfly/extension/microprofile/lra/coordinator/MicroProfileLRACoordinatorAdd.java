/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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

        final LRARecoveryService lraRecoveryService = new LRARecoveryService();
        builder.setInstance(lraRecoveryService);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}