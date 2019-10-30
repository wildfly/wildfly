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

import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.GROUP_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.REAPER_PERIOD;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.TIMEOUT;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.group.impl.GroupingHandlerConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attr : GroupingHandlerDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = registry.getService(serviceName);
        if (service != null) {
            final ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
            if (server.getGroupingHandler() != null) {
                throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.childResourceAlreadyExists(CommonAttributes.GROUPING_HANDLER));
            }
            // the groupingHandler is added as a child of the server resource. Requires a reload to restart the server with the grouping-handler
            if (context.isNormalServer()) {
                context.addStep(new OperationStepHandler() {
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        context.reloadRequired();
                        context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }
        // else the initial subsystem install is not complete and the grouping handler will be added in ServerAdd
    }

    static void addGroupingHandlerConfig(final OperationContext context, final Configuration configuration, final ModelNode model) throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.GROUPING_HANDLER)) {
            final Property prop = model.get(CommonAttributes.GROUPING_HANDLER).asProperty();
            final String name = prop.getName();
            final ModelNode node = prop.getValue();

            final GroupingHandlerConfiguration.TYPE type = GroupingHandlerConfiguration.TYPE.valueOf(GroupingHandlerDefinition.TYPE.resolveModelAttribute(context, node).asString());
            final String address = GROUPING_HANDLER_ADDRESS.resolveModelAttribute(context, node).asString();
            final int timeout = TIMEOUT.resolveModelAttribute(context, node).asInt();
            final long groupTimeout = GROUP_TIMEOUT.resolveModelAttribute(context, node).asLong();
            final long reaperPeriod = REAPER_PERIOD.resolveModelAttribute(context, node).asLong();
            final GroupingHandlerConfiguration conf = new GroupingHandlerConfiguration()
                    .setName(SimpleString.toSimpleString(name))
                    .setType(type)
                    .setAddress(SimpleString.toSimpleString(address))
                    .setTimeout(timeout)
                    .setGroupTimeout(groupTimeout)
                    .setReaperPeriod(reaperPeriod);
            configuration.setGroupingHandlerConfiguration(conf);
        }
    }
}
