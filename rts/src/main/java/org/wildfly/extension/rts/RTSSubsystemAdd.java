/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts;

import static org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition.SOCKET_BINDING_CAPABILITY_NAME;
import static org.wildfly.extension.rts.RTSSubsystemDefinition.RTS_COORDINATOR_CAPABILITY;
import static org.wildfly.extension.rts.RTSSubsystemDefinition.RTS_PARTICIPANT_CAPABILITY;
import static org.wildfly.extension.rts.RTSSubsystemDefinition.RTS_VOLATILE_PARTICIPANT_CAPABILITY;
import static org.wildfly.extension.rts.RTSSubsystemDefinition.XA_RESOURCE_RECOVERY_CAPABILITY;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.rts.configuration.Attribute;
import org.wildfly.extension.rts.deployment.InboundBridgeDeploymentProcessor;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.extension.rts.service.CoordinatorService;
import org.wildfly.extension.rts.service.InboundBridgeService;
import org.wildfly.extension.rts.service.ParticipantService;
import org.wildfly.extension.rts.service.VolatileParticipantService;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Host;

import java.util.function.Supplier;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
final class RTSSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final RTSSubsystemAdd INSTANCE = new RTSSubsystemAdd();

    private RTSSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        RTSLogger.ROOT_LOGGER.trace("RTSSubsystemAdd.populateModel");

        RTSSubsystemDefinition.SERVER.validateAndSet(operation, model);
        RTSSubsystemDefinition.HOST.validateAndSet(operation, model);
        RTSSubsystemDefinition.SOCKET_BINDING.validateAndSet(operation, model);
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        RTSLogger.ROOT_LOGGER.trace("RTSSubsystemAdd.performBoottime");

        registerCoordinatorService(context, model);
        registerParticipantService(context, model);
        registerVolatileParticipantService(context, model);
        registerInboundBridgeService(context);

        registerDeploymentProcessors(context);
    }

    private void registerInboundBridgeService(final OperationContext context) {

        final InboundBridgeService inboundBridgeService = new InboundBridgeService();
        final ServiceBuilder<InboundBridgeService> inboundBridgeServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.INBOUND_BRIDGE, inboundBridgeService);
        inboundBridgeServiceBuilder.requires(context.getCapabilityServiceName(XA_RESOURCE_RECOVERY_CAPABILITY, null));
        inboundBridgeServiceBuilder.requires(RTSSubsystemExtension.PARTICIPANT);

        inboundBridgeServiceBuilder
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private void registerCoordinatorService(final OperationContext context, final ModelNode model) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(RTS_COORDINATOR_CAPABILITY);
        Supplier<Host> hostSupplier = builder.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverName, hostName);
        Supplier<SocketBinding> socketBindingSupplier = builder.requiresCapability(SOCKET_BINDING_CAPABILITY_NAME, SocketBinding.class, socketBindingName);
        final CoordinatorService coordinatorService = new CoordinatorService(hostSupplier, socketBindingSupplier);
        builder.setInstance(coordinatorService)
                .addAliases(RTSSubsystemExtension.COORDINATOR)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private void registerParticipantService(final OperationContext context, final ModelNode model) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(RTS_PARTICIPANT_CAPABILITY);
        Supplier<Host> hostSupplier = builder.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverName, hostName);
        Supplier<SocketBinding> socketBindingSupplier = builder.requiresCapability(SOCKET_BINDING_CAPABILITY_NAME, SocketBinding.class, socketBindingName);
        final ParticipantService participantService = new ParticipantService(hostSupplier, socketBindingSupplier);
        builder.setInstance(participantService)
                .addAliases(RTSSubsystemExtension.PARTICIPANT)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private void registerVolatileParticipantService(final OperationContext context, final ModelNode model) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(RTS_VOLATILE_PARTICIPANT_CAPABILITY);
        Supplier<Host> hostSupplier = builder.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverName, hostName);
        Supplier<SocketBinding> socketBindingSupplier = builder.requiresCapability(SOCKET_BINDING_CAPABILITY_NAME, SocketBinding.class, socketBindingName);
        final VolatileParticipantService volatileParticipantService = new VolatileParticipantService(hostSupplier, socketBindingSupplier);
        builder.setInstance(volatileParticipantService)
                .addAliases(RTSSubsystemExtension.VOLATILE_PARTICIPANT)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private void registerDeploymentProcessors(final OperationContext context) {
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(RTSSubsystemExtension.SUBSYSTEM_NAME, Phase.POST_MODULE,
                        Phase.POST_MODULE_RTS_PROVIDERS, new InboundBridgeDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
