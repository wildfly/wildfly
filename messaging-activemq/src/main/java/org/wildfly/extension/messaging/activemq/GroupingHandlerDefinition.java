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
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.core.server.group.impl.GroupingHandlerConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Grouping handler resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class GroupingHandlerDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition GROUPING_HANDLER_ADDRESS = create("grouping-handler-address", STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition TIMEOUT = create("timeout", LONG)
            .setDefaultValue(new ModelNode(1L * ActiveMQDefaultConfiguration.getDefaultGroupingHandlerTimeout()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition GROUP_TIMEOUT = create("group-timeout", LONG)
            // FIXME Cast to a long until Artemis type is fixed
            .setDefaultValue(new ModelNode(1L * ActiveMQDefaultConfiguration.getDefaultGroupingHandlerGroupTimeout()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition REAPER_PERIOD = create("reaper-period", LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultGroupingHandlerReaperPeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition TYPE = create("type", STRING)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<>(GroupingHandlerConfiguration.TYPE.class, false, true))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { TYPE, GROUPING_HANDLER_ADDRESS, TIMEOUT, GROUP_TIMEOUT, REAPER_PERIOD };

    static final GroupingHandlerDefinition INSTANCE = new GroupingHandlerDefinition();

    private GroupingHandlerDefinition() {
        super(MessagingExtension.GROUPING_HANDLER_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.GROUPING_HANDLER),
                GroupingHandlerAdd.INSTANCE,
                GroupingHandlerRemove.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, GroupingHandlerWriteAttributeHandler.INSTANCE);
            }
        }
    }
}