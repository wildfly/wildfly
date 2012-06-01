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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.JGroupsLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

/**
 * Handler for JGroups subsystem add operations.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat, Inc
 */
public class JGroupsSubsystemAdd extends AbstractAddStepHandler {

    public static final JGroupsSubsystemAdd INSTANCE = new JGroupsSubsystemAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) throws OperationFailedException {
        CommonAttributes.DEFAULT_STACK.validateAndSet(source, target);
        target.get(ModelKeys.STACK).setEmptyObject();
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populate(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        installRuntimeServices(context, operation, model, verificationHandler, newControllers);
    }

    protected void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ROOT_LOGGER.activatingSubsystem();
        ServiceTarget target = context.getServiceTarget();

        // install the protocol defaults service
        ServiceController<ProtocolDefaults> pdsController = installProtocolDefaultsService(target, verificationHandler);
        if (newControllers != null) {
            newControllers.add(pdsController);
        }

        final String stack = CommonAttributes.DEFAULT_STACK.resolveModelAttribute(context, model).asString() ;

        // install the default channel factory service
        ServiceController<ChannelFactory> dcfsController = installDefaultChannelFactoryService(target, stack, verificationHandler);
        if (newControllers != null) {
            newControllers.add(dcfsController);
        }

    }

    protected void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        // remove the ProtocolDefaultsService
        ServiceName protocolDefaultsService = ProtocolDefaultsService.SERVICE_NAME;
        context.removeService(protocolDefaultsService);

        // remove the DefaultChannelFactoryServiceAlias
        ServiceName defaultChannelFactoryService = ChannelFactoryService.getServiceName(null);
        context.removeService(defaultChannelFactoryService);
    }

    protected ServiceController<ProtocolDefaults> installProtocolDefaultsService(ServiceTarget target,
                                                                           ServiceVerificationHandler verificationHandler) {
        final ProtocolDefaultsService service = new ProtocolDefaultsService();
        ServiceBuilder<ProtocolDefaults> protocolDefaultsBuilder =
                target.addService(ProtocolDefaultsService.SERVICE_NAME, service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND) ;
        org.jboss.as.server.Services.addServerExecutorDependency(protocolDefaultsBuilder, service.getExecutorInjector(), false);

        return protocolDefaultsBuilder.install() ;
    }

    protected ServiceController<ChannelFactory> installDefaultChannelFactoryService(ServiceTarget target,
                                                                                    String stack,
                                                                                    ServiceVerificationHandler verificationHandler) {
        InjectedValue<ChannelFactory> factory = new InjectedValue<ChannelFactory>();
        ValueService<ChannelFactory> service = new ValueService<ChannelFactory>(factory);

        ServiceBuilder<ChannelFactory> channelFactoryBuilder =
                target.addService(ChannelFactoryService.getServiceName(null), service)
                .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);

        return channelFactoryBuilder.install() ;
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
