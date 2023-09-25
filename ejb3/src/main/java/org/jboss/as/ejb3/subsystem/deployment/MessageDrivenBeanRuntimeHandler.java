/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.ACTIVATION_CONFIG;
import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.DELIVERY_ACTIVE;
import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.MESSAGE_DESTINATION_LINK;
import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.MESSAGE_DESTINATION_TYPE;
import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.MESSAGING_TYPE;
import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.START_DELIVERY;
import static org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition.STOP_DELIVERY;

import java.util.Properties;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;
import org.jboss.msc.service.ServiceController;

/**
 * Handles operations that provide runtime management of a {@link MessageDrivenComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author Flavia Rainone
 */
public class MessageDrivenBeanRuntimeHandler extends AbstractEJBComponentRuntimeHandler<MessageDrivenComponent> {

    public static final MessageDrivenBeanRuntimeHandler INSTANCE = new MessageDrivenBeanRuntimeHandler();

    private MessageDrivenBeanRuntimeHandler() {
        super(EJBComponentType.MESSAGE_DRIVEN, MessageDrivenComponent.class);
    }


    @Override
    protected void executeReadAttribute(String attributeName, OperationContext context, MessageDrivenComponent component, PathAddress address) {
        final MessageDrivenComponentDescription componentDescription = (MessageDrivenComponentDescription) component.getComponentDescription();
        final ModelNode result = context.getResult();
        if (DELIVERY_ACTIVE.getName().equals(attributeName)) {
            result.set(component.isDeliveryActive());
        } else if (MESSAGING_TYPE.getName().equals(attributeName)) {
            result.set(componentDescription.getMessageListenerInterfaceName());
        } else if (MESSAGE_DESTINATION_TYPE.getName().equals(attributeName)) {
            final MessageDrivenBeanMetaData descriptorData = componentDescription.getDescriptorData();
            if (descriptorData != null) {
                final String destinationType = descriptorData.getMessageDestinationType();
                if (destinationType != null) {
                    result.set(destinationType);
                }
            }
        } else if (MESSAGE_DESTINATION_LINK.getName().equals(attributeName)) {
            final MessageDrivenBeanMetaData descriptorData = componentDescription.getDescriptorData();
            if (descriptorData != null) {
                final String messageDestinationLink = descriptorData.getMessageDestinationLink();
                if (messageDestinationLink != null) {
                    result.set(messageDestinationLink);
                }
            }
        } else if (ACTIVATION_CONFIG.getName().equals(attributeName)) {
            final Properties activationProps = componentDescription.getActivationProps();
            for (String k : activationProps.stringPropertyNames()) {
                result.add(k, activationProps.getProperty(k));
            }
        } else {
            super.executeReadAttribute(attributeName, context, component, address);
        }
    }

    protected boolean isOperationReadOnly(String opName) {
        if (START_DELIVERY.equals(opName) ||
                STOP_DELIVERY.equals(opName)) {
            return false;
        }
        return super.isOperationReadOnly(opName);
    }

    @Override
    protected void executeAgainstComponent(OperationContext context, ModelNode operation, MessageDrivenComponent component, String opName, PathAddress address) throws OperationFailedException {
        if (START_DELIVERY.equals(opName)) {
            if (component.isDeliveryControlled()) {
                context.getServiceRegistry(true).getRequiredService(component.getDeliveryControllerName()).setMode(ServiceController.Mode.PASSIVE);
            } else {
                component.startDelivery();
            }
        } else if (STOP_DELIVERY.equals(opName)) {
            if (component.isDeliveryControlled()) {
                context.getServiceRegistry(true).getRequiredService(component.getDeliveryControllerName()).setMode(
                        ServiceController.Mode.NEVER);
            } else {
                component.stopDelivery();
            }
        } else {
            super.executeAgainstComponent(context, operation, component, opName, address);
        }
    }
}
