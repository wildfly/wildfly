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
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.rts.configuration.Attribute;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.extension.rts.service.CoordinatorService;
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

        final String serverName = model.get(Attribute.SERVER.getLocalName()).asString();
        final String hostName = model.get(Attribute.HOST.getLocalName()).asString();
        final String socketBindingName = model.get(Attribute.SOCKET_BINDING.getLocalName()).asString();
        final CoordinatorService coordinatorService = new CoordinatorService();
        final ParticipantService participantService = new ParticipantService();

        final ServiceBuilder<CoordinatorService> coordinatorServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.COORDINATOR, coordinatorService)
                .addListener(verificationHandler)
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        coordinatorService.getInjectedHost());

        final ServiceBuilder<ParticipantService> participantServiceBuilder = context
                .getServiceTarget()
                .addService(RTSSubsystemExtension.PARTICIPANT, participantService)
                .addListener(verificationHandler)
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName), SocketBinding.class,
                        participantService.getInjectedSocketBinding())
                .addDependency(UndertowService.virtualHostName(serverName, hostName), Host.class,
                        participantService.getInjectedHost());

        coordinatorServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        participantServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        final ServiceController<CoordinatorService> coordinatorServiceController = coordinatorServiceBuilder.install();
        final ServiceController<ParticipantService> participantServiceController = participantServiceBuilder.install();

        if (newControllers != null) {
            newControllers.add(coordinatorServiceController);
            newControllers.add(participantServiceController);
        }
    }

}