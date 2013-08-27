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

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
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
        if (RTSLogger.ROOT_LOGGER.isTraceEnabled()) {
            RTSLogger.ROOT_LOGGER.trace("RTSSubsystemAdd.populateModel");
        }

        RTSSubsystemDefinition.SERVER.validateAndSet(operation, model);
        RTSSubsystemDefinition.HOST.validateAndSet(operation, model);
        RTSSubsystemDefinition.SOCKET_BINDING.validateAndSet(operation, model);
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        if (RTSLogger.ROOT_LOGGER.isTraceEnabled()) {
            RTSLogger.ROOT_LOGGER.trace("RTSSubsystemAdd.performBoottime");
        }

        registerCoordinatorService(context, model, verificationHandler, newControllers);
        registerParticipantService(context, model, verificationHandler, newControllers);
        registerInboundBridgeService(context, verificationHandler, newControllers);

        registerDeploymentProcessors(context);
    }

    private void registerInboundBridgeService(final OperationContext context,
              final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final InboundBridgeService inboundBridgeService = new InboundBridgeService();
        final ServiceBuilder<InboundBridgeService> inboundBridgeServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.INBOUND_BRIDGE, inboundBridgeService)
                .addListener(verificationHandler)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER);

        inboundBridgeServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<InboundBridgeService> inboundBridgeServiceController = inboundBridgeServiceBuilder.install();

        if (newControllers != null) {
            newControllers.add(inboundBridgeServiceController);
        }
    }

    private void registerCoordinatorService(final OperationContext context, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final CoordinatorService coordinatorService = new CoordinatorService();
        final ServiceBuilder<CoordinatorService> coordinatorServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.COORDINATOR, coordinatorService)
                .addListener(verificationHandler)
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        coordinatorService.getInjectedHost());

        coordinatorServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<CoordinatorService> coordinatorServiceController = coordinatorServiceBuilder.install();

        if (newControllers != null) {
            newControllers.add(coordinatorServiceController);
        }
    }

    private void registerParticipantService(final OperationContext context, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final ParticipantService participantService = new ParticipantService();
        final ServiceBuilder<ParticipantService> participantServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.PARTICIPANT, participantService)
                .addListener(verificationHandler)
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName), SocketBinding.class,
                        participantService.getInjectedSocketBinding())
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        participantService.getInjectedHost());

        participantServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<ParticipantService> participantServiceController = participantServiceBuilder.install();

        if (newControllers != null) {
            newControllers.add(participantServiceController);
        }
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