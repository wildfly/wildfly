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

package org.wildfly.extension.messaging.activemq;

import java.util.List;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.apache.activemq.artemis.core.config.TransformerConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = registry.getService(serviceName);
        if (service != null) {

            // The original subsystem initialization is complete; use the control object to create the divert
            if (service.getState() != ServiceController.State.UP) {
                throw MessagingLogger.ROOT_LOGGER.invalidServiceState(serviceName, ServiceController.State.UP, service.getState());
            }

            final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

            DivertConfiguration divertConfiguration = createDivertConfiguration(context, name, model);

            ActiveMQServerControl serverControl = ActiveMQServer.class.cast(service.getValue()).getActiveMQServerControl();
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
        DivertConfiguration config = new DivertConfiguration()
                .setName(name)
                .setRoutingName(routingName)
                .setAddress(address)
                .setForwardingAddress(forwardingAddress)
                .setExclusive(exclusive)
                .setFilterString(filter);
        final ModelNode transformerClassName =  CommonAttributes.TRANSFORMER_CLASS_NAME.resolveModelAttribute(context, model);
        if (transformerClassName.isDefined()) {
            config.setTransformerConfiguration(new TransformerConfiguration(transformerClassName.asString()));
        }
        return config;
    }

    static void createDivert(String name, DivertConfiguration divertConfiguration, ActiveMQServerControl serverControl) {
        try {
            String transformerClassName = divertConfiguration.getTransformerConfiguration() != null ? divertConfiguration.getTransformerConfiguration().getClassName() : null;
            serverControl.createDivert(name, divertConfiguration.getRoutingName(), divertConfiguration.getAddress(),
                    divertConfiguration.getForwardingAddress(), divertConfiguration.isExclusive(),
                    divertConfiguration.getFilterString(), transformerClassName);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TODO should this be an OFE instead?
            throw new RuntimeException(e);
        }
    }
}