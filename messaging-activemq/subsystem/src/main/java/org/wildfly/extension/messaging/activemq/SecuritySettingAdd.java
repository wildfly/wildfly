/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.getActiveMQServer;

import java.util.HashSet;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationStepHandler} for adding a new security setting.
 *
 * @author Emanuel Muckenhuber
 */
class SecuritySettingAdd extends AbstractAddStepHandler {

    static final SecuritySettingAdd INSTANCE = new SecuritySettingAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime() && !context.isBooting();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final ActiveMQServer server = getActiveMQServer(context, operation);
        if (server != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final String match = address.getLastElement().getValue();
            server.getSecurityRepository().addMatch(match, new HashSet<>());
        }
    }

}
