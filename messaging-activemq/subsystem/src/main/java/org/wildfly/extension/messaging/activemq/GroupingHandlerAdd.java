/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Handler for adding a grouping handler.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class GroupingHandlerAdd extends AbstractAddStepHandler {

    public static final GroupingHandlerAdd INSTANCE = new GroupingHandlerAdd(GroupingHandlerDefinition.ATTRIBUTES);

    private GroupingHandlerAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime() && !context.isBooting();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = registry.getService(serviceName);
        if (service != null) {
            final ActiveMQServer server = ActiveMQServer.class.cast(ActiveMQBroker.class.cast(service.getValue()).getDelegate());
            if (server.getGroupingHandler() != null) {
                throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.childResourceAlreadyExists(CommonAttributes.GROUPING_HANDLER));
            }
            // the groupingHandler is added as a child of the server resource. Requires a reload to restart the server with the grouping-handler
            if (context.isNormalServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        context.reloadRequired();
                        context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }
        // else the initial subsystem install is not complete and the grouping handler will be added in ServerAdd
    }
}
