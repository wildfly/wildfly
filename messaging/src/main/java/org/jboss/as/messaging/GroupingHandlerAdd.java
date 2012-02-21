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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Locale;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.group.impl.GroupingHandlerConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
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
public class GroupingHandlerAdd implements OperationStepHandler, DescriptionProvider {

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, subModel);

        return operation;
    }

    public static final GroupingHandlerAdd INSTANCE = new GroupingHandlerAdd();

    private GroupingHandlerAdd() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress ourAddress = PathAddress.pathAddress(operation.require(OP_ADDR));

        final Resource subsystemRootResource = context.readResourceFromRoot(ourAddress.subAddress(0, ourAddress.size() - 1));
        if (subsystemRootResource.hasChildren(CommonAttributes.GROUPING_HANDLER)) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.childResourceAlreadyExists(CommonAttributes.GROUPING_HANDLER)));
        }
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();
        for (final AttributeDefinition attributeDefinition : CommonAttributes.GROUPING_HANDLER_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
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

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.revertReloadRequired();
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getGroupingHandlerAdd(locale);
    }

    static void addGroupingHandlerConfig(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.GROUPING_HANDLER)) {
            Property prop = model.get(CommonAttributes.BROADCAST_GROUP).asProperty();
            configuration.setGroupingHandlerConfiguration(createGroupingHandlerConfiguration(context, prop.getName(), prop.getValue()));
        }
    }

    static GroupingHandlerConfiguration createGroupingHandlerConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final GroupingHandlerConfiguration.TYPE type = GroupingHandlerConfiguration.TYPE.valueOf(CommonAttributes.TYPE.resolveModelAttribute(context, model).asString());
        final String address = CommonAttributes.GROUPING_HANDLER_ADDRESS.resolveModelAttribute(context, model).asString();
        final int timeout = CommonAttributes.TIMEOUT.resolveModelAttribute(context, model).asInt();
        return new GroupingHandlerConfiguration(SimpleString.toSimpleString(name), type, SimpleString.toSimpleString(address), timeout);
    }
}
