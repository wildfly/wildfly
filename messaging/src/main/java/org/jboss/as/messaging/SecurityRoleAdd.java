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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@code OperationStepHandler} for adding a new security role.
 *
 * @author Emanuel Muckenhuber
 */
class SecurityRoleAdd implements OperationStepHandler, DescriptionProvider {

    static final SecurityRoleAdd INSTANCE = new SecurityRoleAdd();

    static final AttributeDefinition SEND = new RoleAttributeDefinition("send");
    static final AttributeDefinition CONSUME = new RoleAttributeDefinition("consume");
    static final AttributeDefinition CREATE_DURABLE_QUEUE = new RoleAttributeDefinition("create-durable-queue", "createDurableQueue");
    static final AttributeDefinition DELETE_DURABLE_QUEUE = new RoleAttributeDefinition("delete-durable-queue", "deleteDurableQueue");
    static final AttributeDefinition CREATE_NON_DURABLE_QUEUE = new RoleAttributeDefinition("create-non-durable-queue", "createNonDurableQueue");
    static final AttributeDefinition DELETE_NON_DURABLE_QUEUE = new RoleAttributeDefinition("delete-non-durable-queue", "deleteNonDurableQueue");
    static final AttributeDefinition MANAGE = new RoleAttributeDefinition("manage");

    static final AttributeDefinition[] ROLE_ATTRIBUTES = new AttributeDefinition[] {SEND, CONSUME, CREATE_DURABLE_QUEUE, DELETE_DURABLE_QUEUE,
                    CREATE_NON_DURABLE_QUEUE, DELETE_NON_DURABLE_QUEUE, MANAGE};

    static final Map<String, AttributeDefinition> ROLE_ATTRIBUTES_BY_XML_NAME;

    static {
        Map<String, AttributeDefinition> robxn = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition attr : ROLE_ATTRIBUTES) {
            robxn.put(attr.getXmlName(), attr);
        }
        // Legacy xml names
        robxn.put("createTempQueue", CREATE_NON_DURABLE_QUEUE);
        robxn.put("deleteTempQueue", DELETE_NON_DURABLE_QUEUE);
        ROLE_ATTRIBUTES_BY_XML_NAME = Collections.unmodifiableMap(robxn);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = resource.getModel();
        for(final AttributeDefinition attribute : ROLE_ATTRIBUTES) {
            attribute.validateAndSet(operation, subModel);
        }
        if(context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                    final HornetQServer server = getServer(context, operation);
                    if(server != null) {
                        final String match = address.getElement(address.size() - 2).getValue();
                        final String role = address.getLastElement().getValue();
                        final Set<Role> roles = server.getSecurityRepository().getMatch(match);
                        roles.add(transform(context, role, subModel));
                        server.getSecurityRepository().addMatch(match, roles);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return MessagingDescriptions.getSecurityRoleAdd(locale);
    }

    static ModelNode createAddOperation(final ModelNode address, final ModelNode subModel) {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        for(final AttributeDefinition def : ROLE_ATTRIBUTES) {
            if(subModel.hasDefined(def.getName())) {
                operation.get(def.getName()).set(subModel.get(def.getName()));
            }
        }
        return operation;
    }

    static Role transform(final OperationContext context, final String name, final ModelNode node) throws OperationFailedException {
        final boolean send = SEND.resolveModelAttribute(context, node).asBoolean();
        final boolean consume = CONSUME.resolveModelAttribute(context, node).asBoolean();
        final boolean createDurableQueue = CREATE_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean deleteDurableQueue = DELETE_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean createNonDurableQueue = CREATE_NON_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean deleteNonDurableQueue = DELETE_NON_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean manage = MANAGE.resolveModelAttribute(context, node).asBoolean();
        return new Role(name, send, consume, createDurableQueue, deleteDurableQueue, createNonDurableQueue, deleteNonDurableQueue, manage);
    }

    static HornetQServer getServer(final OperationContext context, ModelNode operation) {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> controller = context.getServiceRegistry(true).getService(hqServiceName);
        if(controller != null) {
            return HornetQServer.class.cast(controller.getValue());
        }
        return null;
    }

    static final class RoleAttributeDefinition extends SimpleAttributeDefinition {

        RoleAttributeDefinition(final String name) {
            this(name, name);
        }

        RoleAttributeDefinition(final String name, final String xmlName) {
            super(name, xmlName, new ModelNode().set(false), ModelType.BOOLEAN, false, false, MeasurementUnit.NONE);
        }
    }

}
