/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers.isServiceInstalled;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public class ConnectorAdd extends AbstractAddStepHandler {

    public ConnectorAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    /**
     * Nothing to do if is it an external connector as we don't need to update
     * the underlying broker.
     *
     * @param context operation context
     * @return {@code true} if {@code performRuntime} should be invoked;
     * {@code false} otherwise.
     */
    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime() && !context.isBooting() && !MessagingServices.isSubsystemResource(context) && isServiceInstalled(context);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        try {
            TransportConfigOperationHandlers.processConnector(context, model);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException ex) {
            throw new OperationFailedException(ex);
        }
    }
}
