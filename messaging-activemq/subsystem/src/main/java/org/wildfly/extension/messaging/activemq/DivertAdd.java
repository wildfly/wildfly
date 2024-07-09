/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;


import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.apache.activemq.artemis.core.config.TransformerConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

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
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime() && !context.isBooting();
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

            ActiveMQServerControl serverControl = ActiveMQBroker.class.cast(service.getValue()).getActiveMQServerControl();
            createDivert(name, divertConfiguration, serverControl);

        }
        // else the initial subsystem install is not complete; MessagingSubsystemAdd will add a
        // handler that calls addDivertConfigs
    }

    static DivertConfiguration createDivertConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
        final String routingName = DivertDefinition.ROUTING_NAME.resolveModelAttribute(context, model).asStringOrNull();
        final String address = DivertDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final String forwardingAddress = DivertDefinition.FORWARDING_ADDRESS.resolveModelAttribute(context, model).asString();
        final boolean exclusive = DivertDefinition.EXCLUSIVE.resolveModelAttribute(context, model).asBoolean();
        final String filter = CommonAttributes.FILTER.resolveModelAttribute(context, model).asStringOrNull();
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