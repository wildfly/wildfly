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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.List;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.group.impl.GroupingHandlerConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for adding a broadcast group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class GroupingHandlerAdd extends AbstractAddStepHandler {

    public static final GroupingHandlerAdd INSTANCE = new GroupingHandlerAdd();

    private GroupingHandlerAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attr : GroupingHandlerDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        PathAddress ourAddress = PathAddress.pathAddress(operation.require(OP_ADDR));

        final Resource subsystemRootResource = context.readResourceFromRoot(ourAddress.subAddress(0, ourAddress.size() - 1));
        if (subsystemRootResource.hasChildren(CommonAttributes.GROUPING_HANDLER)) { //todo this probably is not needed anymore, should be verifed
            throw new OperationFailedException(new ModelNode().set(MESSAGES.childResourceAlreadyExists(CommonAttributes.GROUPING_HANDLER)));
        }

        if (context.isNormalServer()) {

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ServiceRegistry registry = context.getServiceRegistry(false);
                    final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                    ServiceController<?> hqService = registry.getService(hqServiceName);
                    if (hqService != null) {
                        context.reloadRequired();
                    }
                    // else MessagingSubsystemAdd will add a handler that calls addBroadcastGroupConfigs

                    context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);

                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }

    static void addGroupingHandlerConfig(final OperationContext context, final Configuration configuration, final ModelNode model) throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.GROUPING_HANDLER)) {
            Property prop = model.get(CommonAttributes.GROUPING_HANDLER).asProperty();
            configuration.setGroupingHandlerConfiguration(createGroupingHandlerConfiguration(context, prop.getName(), prop.getValue()));
        }
    }

    static GroupingHandlerConfiguration createGroupingHandlerConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final GroupingHandlerConfiguration.TYPE type = GroupingHandlerConfiguration.TYPE.valueOf(GroupingHandlerDefinition.TYPE.resolveModelAttribute(context, model).asString());
        final String address = GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS.resolveModelAttribute(context, model).asString();
        final int timeout = GroupingHandlerDefinition.TIMEOUT.resolveModelAttribute(context, model).asInt();
        return new GroupingHandlerConfiguration(SimpleString.toSimpleString(name), type, SimpleString.toSimpleString(address), timeout);
    }
}
