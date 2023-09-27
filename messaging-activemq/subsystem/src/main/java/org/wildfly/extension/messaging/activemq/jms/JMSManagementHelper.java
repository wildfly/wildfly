/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
