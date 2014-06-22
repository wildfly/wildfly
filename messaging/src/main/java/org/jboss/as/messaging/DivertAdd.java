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

package org.jboss.as.messaging;

import java.util.List;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.DivertConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for adding a divert.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DivertAdd extends AbstractAddStepHandler {

    public static final DivertAdd INSTANCE = new DivertAdd(DivertDefinition.ATTRIBUTES);

    private DivertAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = registry.getService(hqServiceName);
        if (hqService != null) {

            // The original subsystem initialization is complete; use the control object to create the divert
            if (hqService.getState() != ServiceController.State.UP) {
                throw MessagingLogger.ROOT_LOGGER.invalidServiceState(hqServiceName, ServiceController.State.UP, hqService.getState());
            }

            final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

            DivertConfiguration divertConfiguration = createDivertConfiguration(context, name, model);

            HornetQServerControl serverControl = HornetQServer.class.cast(hqService.getValue()).getHornetQServerControl();
            createDivert(name, divertConfiguration, serverControl);

        }
        // else the initial subsystem install is not complete; MessagingSubsystemAdd will add a
        // handler that calls addDivertConfigs
    }

    static void addDivertConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.DIVERT)) {
            final List<DivertConfiguration> configs = configuration.getDivertConfigurations();
            for (Property prop : model.get(CommonAttributes.DIVERT).asPropertyList()) {
                configs.add(createDivertConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static DivertConfiguration createDivertConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
        final ModelNode routingNode = DivertDefinition.ROUTING_NAME.resolveModelAttribute(context, model);
        final String routingName = routingNode.isDefined() ? routingNode.asString() : null;
        final String address = DivertDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final String forwardingAddress = DivertDefinition.FORWARDING_ADDRESS.resolveModelAttribute(context, model).asString();
        final boolean exclusive = DivertDefinition.EXCLUSIVE.resolveModelAttribute(context, model).asBoolean();
        final ModelNode filterNode = CommonAttributes.FILTER.resolveModelAttribute(context, model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final ModelNode transformerNode =  CommonAttributes.TRANSFORMER_CLASS_NAME.resolveModelAttribute(context, model);
        final String transformerClassName = transformerNode.isDefined() ? transformerNode.asString() : null;
        return new DivertConfiguration(name, routingName, address, forwardingAddress, exclusive, filter, transformerClassName);
    }

    static void createDivert(String name, DivertConfiguration divertConfiguration, HornetQServerControl serverControl) {
        try {
            serverControl.createDivert(name, divertConfiguration.getRoutingName(), divertConfiguration.getAddress(),
                    divertConfiguration.getForwardingAddress(), divertConfiguration.isExclusive(),
                    divertConfiguration.getFilterString(), divertConfiguration.getTransformerClassName());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TODO should this be an OFE instead?
            throw new RuntimeException(e);
        }
    }
}