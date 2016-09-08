/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.rts;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.txn.service.TxnServices;
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
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

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
                .addService(RTSSubsystemExtension.INBOUND_BRIDGE, inboundBridgeService)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER)
                .addDependency(RTSSubsystemExtension.PARTICIPANT);

        inboundBridgeServiceBuilder
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private void registerCoordinatorService(final OperationContext context, final ModelNode model) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final CoordinatorService coordinatorService = new CoordinatorService();
        final ServiceBuilder<CoordinatorService> coordinatorServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.COORDINATOR, coordinatorService)
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName), SocketBinding.class,
                        coordinatorService.getInjectedSocketBinding())
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        coordinatorService.getInjectedHost());

        coordinatorServiceBuilder
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private void registerParticipantService(final OperationContext context, final ModelNode model) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final ParticipantService participantService = new ParticipantService();
        final ServiceBuilder<ParticipantService> participantServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.PARTICIPANT, participantService)
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName), SocketBinding.class,
                        participantService.getInjectedSocketBinding())
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        participantService.getInjectedHost());

        participantServiceBuilder
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();


    }

    private void registerVolatileParticipantService(final OperationContext context, final ModelNode model) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final VolatileParticipantService volatileParticipantService = new VolatileParticipantService();
        final ServiceBuilder<VolatileParticipantService> volatileParticipantServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.VOLATILE_PARTICIPANT, volatileParticipantService)
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName), SocketBinding.class,
                        volatileParticipantService.getInjectedSocketBinding())
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        volatileParticipantService.getInjectedHost());

        volatileParticipantServiceBuilder
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