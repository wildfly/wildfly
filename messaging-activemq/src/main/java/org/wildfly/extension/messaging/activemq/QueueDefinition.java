/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.LONG;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.RUNTIME_QUEUE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
 * Queue resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class QueueDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition ADDRESS = create("queue-address", ModelType.STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = { ADDRESS, CommonAttributes.FILTER, CommonAttributes.DURABLE };

    public static final SimpleAttributeDefinition EXPIRY_ADDRESS = create(CommonAttributes.EXPIRY_ADDRESS)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition DEAD_LETTER_ADDRESS = create(CommonAttributes.DEAD_LETTER_ADDRESS)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition ID= create("id", LONG)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition[] READONLY_ATTRIBUTES = { CommonAttributes.PAUSED, CommonAttributes.TEMPORARY, ID, DEAD_LETTER_ADDRESS, EXPIRY_ADDRESS };

    static final AttributeDefinition[] METRICS = { CommonAttributes.MESSAGE_COUNT, CommonAttributes.DELIVERING_COUNT, CommonAttributes.MESSAGES_ADDED,
            CommonAttributes.SCHEDULED_COUNT, CommonAttributes.CONSUMER_COUNT
    };

    private final boolean registerRuntimeOnly;

    QueueDefinition(final boolean registerRuntimeOnly,
                    final PathElement path) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.QUEUE),
                path == MessagingExtension.RUNTIME_QUEUE_PATH ? null : QueueAdd.INSTANCE,
                path == MessagingExtension.RUNTIME_QUEUE_PATH ? null : QueueRemove.INSTANCE,
                path == MessagingExtension.RUNTIME_QUEUE_PATH);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public boolean isRuntime() {
        return getPathElement() == MessagingExtension.RUNTIME_QUEUE_PATH;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                if (isRuntime()) {
                    AttributeDefinition readOnlyRuntimeAttr = create(attr)
                            .setStorageRuntime()
                            .build();
                    registry.registerReadOnlyAttribute(readOnlyRuntimeAttr, QueueReadAttributeHandler.RUNTIME_INSTANCE);
                } else {
                    registry.registerReadOnlyAttribute(attr, null);
                }
            }
        }

        for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, QueueReadAttributeHandler.INSTANCE);
        }

        for (AttributeDefinition metric : METRICS) {
            registry.registerMetric(metric, QueueReadAttributeHandler.INSTANCE);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            QueueControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        if (isRuntime()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(MessagingExtension.QUEUE_ACCESS_CONSTRAINT);
        }
    }

    /**
     * [AS7-5850] Core queues created with ActiveMQ API does not create WildFly resources
     *
     * For backwards compatibility if an operation is invoked on a queue that has no corresponding resources,
     * we forward the operation to the corresponding runtime-queue resource (which *does* exist).
     *
     * @return true if the operation is forwarded to the corresponding runtime-queue resource, false else.
     */
    static boolean forwardToRuntimeQueue(OperationContext context, ModelNode operation, OperationStepHandler handler) {
        PathAddress address = context.getCurrentAddress();

        // do not forward if the current operation is for a runtime-queue already:
        if (RUNTIME_QUEUE.equals(address.getLastElement().getKey())) {
            return false;
        }

        String queueName = address.getLastElement().getValue();
        System.out.println(">>> 1 queueName = " + queueName);
        if (queueName.startsWith("jms.queue.")) {
            queueName = queueName.substring("jms.queue.".length());
        }
        System.out.println(">>> 2 queueName = " + queueName);


        PathAddress activeMQPathAddress = MessagingServices.getActiveMQServerPathAddress(address);
        if (context.readResourceFromRoot(activeMQPathAddress, false).hasChild(address.getLastElement())) {
            return false;
        } else {
            // there is no registered queue resource, forward to the runtime-queue address instead
            ModelNode forwardOperation = operation.clone();
            forwardOperation.get(ModelDescriptionConstants.OP_ADDR).set(activeMQPathAddress.append(RUNTIME_QUEUE, queueName).toModelNode());
            context.addStep(forwardOperation, handler, OperationContext.Stage.RUNTIME, true);
            return true;
        }
    }
}
