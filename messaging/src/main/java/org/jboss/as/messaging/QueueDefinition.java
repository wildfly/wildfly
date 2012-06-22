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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.LONG;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;


/**
 * Queue resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class QueueDefinition extends SimpleResourceDefinition {

    public static final AttributeDefinition ADDRESS = create("queue-address", ModelType.STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setRestartAllServices()
            .build();

    static final AttributeDefinition[] ATTRIBUTES = { ADDRESS, CommonAttributes.FILTER, CommonAttributes.DURABLE };

    static final AttributeDefinition ID= create("id", LONG)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition[] READONLY_ATTRIBUTES = { CommonAttributes.PAUSED, CommonAttributes.TEMPORARY, ID };

    static final AttributeDefinition[] METRICS = { CommonAttributes.MESSAGE_COUNT, CommonAttributes.DELIVERING_COUNT, CommonAttributes.MESSAGES_ADDED,
            CommonAttributes.SCHEDULED_COUNT, CommonAttributes.CONSUMER_COUNT
            };

    private final boolean registerRuntimeOnly;

    public QueueDefinition(final boolean registerRuntimeOnly) {
        super(PathElement.pathElement(CommonAttributes.QUEUE),
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.QUEUE),
                QueueAdd.INSTANCE,
                QueueRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, QueueConfigurationWriteHandler.INSTANCE);
            }
        }

        if (registerRuntimeOnly) {
            for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
                registry.registerReadOnlyAttribute(attr, QueueReadAttributeHandler.INSTANCE);
            }

            for (AttributeDefinition metric : METRICS) {
                registry.registerMetric(metric, QueueReadAttributeHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            QueueControlHandler.INSTANCE.registerOperations(registry);
        }
    }
}