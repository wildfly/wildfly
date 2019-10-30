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
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.dmr.ModelNode;

/**
 * Grouping handler resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class GroupingHandlerDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.GROUPING_HANDLER);

    public static final SimpleAttributeDefinition GROUPING_HANDLER_ADDRESS = create("grouping-handler-address", STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    // FIXME GroupingHanderConfiguration timeout is an int (instead of a long). Use an int until HornetQ conf is fixed
    // [HORNETQ-885]
    public static final SimpleAttributeDefinition TIMEOUT = create("timeout", INT)
            .setDefaultValue(new ModelNode(5000))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition GROUP_TIMEOUT = create("group-timeout", LONG)
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition REAPER_PERIOD = create("reaper-period", LONG)
            .setDefaultValue(new ModelNode(30000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition TYPE = create("type", STRING)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<>(TYPE.class, false, true))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { TYPE, GROUPING_HANDLER_ADDRESS, TIMEOUT, GROUP_TIMEOUT, REAPER_PERIOD };

    static final GroupingHandlerDefinition INSTANCE = new GroupingHandlerDefinition();

    private GroupingHandlerDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.GROUPING_HANDLER),
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    // copied from HornetQ to avoid import HornetQ artifacts just to define attribute constants and enum validator
    private enum TYPE  {
        LOCAL("LOCAL"), REMOTE("REMOTE");

        private String type;

        TYPE(final String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}