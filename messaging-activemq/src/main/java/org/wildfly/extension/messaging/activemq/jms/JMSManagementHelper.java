/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.messaging.activemq.AbstractQueueControlHandler.PRIORITY_VALIDATOR;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class JMSManagementHelper {

    private static class DeliveryModeValidator extends StringLengthValidator implements AllowedValuesValidator {

        public DeliveryModeValidator() {
            super(1);
        }

        @Override
        public List<ModelNode> getAllowedValues() {
            List<ModelNode> values = new ArrayList<>();
            values.add(new ModelNode("PERSISTENT"));
            values.add(new ModelNode("NON_PERSISTENT"));
            return values;
        }
    }

    protected static final AttributeDefinition[] JMS_MESSAGE_PARAMETERS = new AttributeDefinition[] {
        createNonEmptyStringAttribute("JMSMessageID"),
                create("JMSPriority", INT)
                        .setValidator(PRIORITY_VALIDATOR)
                        .build(),
                create("JMSTimestamp", LONG)
                        .build(),
                create("JMSExpiration", LONG)
                        .build(),
                create("JMSDeliveryMode", STRING)
                        .build(),
                create("JMSDeliveryMode", STRING)
                        .setValidator(new DeliveryModeValidator())
                        .build()
    };
}
