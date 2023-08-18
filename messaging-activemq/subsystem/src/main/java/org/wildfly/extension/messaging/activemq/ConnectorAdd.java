/*
 * Copyright 2023 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
