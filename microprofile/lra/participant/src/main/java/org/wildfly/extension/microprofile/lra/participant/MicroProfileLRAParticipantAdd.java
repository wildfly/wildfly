/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.microprofile.lra.participant._private.MicroProfileLRAParticipantLogger;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantDeploymentDependencyProcessor;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantDeploymentSetupProcessor;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantJaxrsDeploymentUnitProcessor;
import org.wildfly.extension.microprofile.lra.participant.service.LRAParticipantService;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantSubsystemDefinition.ATTRIBUTES;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantSubsystemDefinition.COORDINATOR_URL_PROP;

class MicroProfileLRAParticipantAdd extends AbstractBoottimeAddStepHandler {

    MicroProfileLRAParticipantAdd() {
        super(Arrays.asList(ATTRIBUTES));
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);
        final String url = MicroProfileLRAParticipantSubsystemDefinition.LRA_COORDINATOR_URL.resolveModelAttribute(context, model).asString();
        System.setProperty(COORDINATOR_URL_PROP, url);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {

                // TODO Put these into Phase.java https://issues.redhat.com/browse/WFCORE-5559
                final int STRUCTURE_MICROPROFILE_LRA_PARTICIPANT = 0x2400;
                final int DEPENDENCIES_MICROPROFILE_LRA_PARTICIPANT = 0x18D0;

                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.STRUCTURE, STRUCTURE_MICROPROFILE_LRA_PARTICIPANT, new LRAParticipantDeploymentSetupProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_MICROPROFILE_LRA_PARTICIPANT, new LRAParticipantDeploymentDependencyProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_SCANNING, new LRAParticipantJaxrsDeploymentUnitProcessor());
            }
        }, RUNTIME);

        registerParticipantProxyService(context, model);

        MicroProfileLRAParticipantLogger.LOGGER.activatingSubsystem(url);
    }

    private void registerParticipantProxyService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget()
            .addCapability(MicroProfileLRAParticipantSubsystemDefinition.LRA_PARTICIPANT_CAPABILITY);

        builder.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        String serverModelValue = MicroProfileLRAParticipantSubsystemDefinition.PROXY_SERVER.resolveModelAttribute(context, model).asString();
        String hostModelValue = MicroProfileLRAParticipantSubsystemDefinition.PROXY_HOST.resolveModelAttribute(context, model).asString();
        Supplier<Host> hostSupplier = builder.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverModelValue, hostModelValue);

        final LRAParticipantService lraParticipantProxyService = new LRAParticipantService(hostSupplier);

        builder.setInstance(lraParticipantProxyService);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}