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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.STRING;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingExtension;


/**
 * JMS Topic resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class JMSTopicDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.JMS_TOPIC);

    public static final AttributeDefinition[] ATTRIBUTES = { CommonAttributes.DESTINATION_ENTRIES };

    /**
     * Attributes for deployed JMS topic are stored in runtime
     */
    private static AttributeDefinition[] getDeploymentAttributes() {
        return new AttributeDefinition[] {
                new PrimitiveListAttributeDefinition.Builder(CommonAttributes.DESTINATION_ENTRIES).setStorageRuntime().build()
        };
    }

    static final AttributeDefinition TOPIC_ADDRESS = create(CommonAttributes.TOPIC_ADDRESS, STRING)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition[] READONLY_ATTRIBUTES = { TOPIC_ADDRESS, CommonAttributes.TEMPORARY };

    static final AttributeDefinition DURABLE_MESSAGE_COUNT = create(CommonAttributes.DURABLE_MESSAGE_COUNT, INT)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition NON_DURABLE_MESSAGE_COUNT = create(CommonAttributes.NON_DURABLE_MESSAGE_COUNT, INT)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition SUBSCRIPTION_COUNT = create(CommonAttributes.SUBSCRIPTION_COUNT, INT)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition DURABLE_SUBSCRIPTION_COUNT = create(CommonAttributes.DURABLE_SUBSCRIPTION_COUNT, INT)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition NON_DURABLE_SUBSCRIPTION_COUNT = create(CommonAttributes.NON_DURABLE_SUBSCRIPTION_COUNT, INT)
            .setStorageRuntime()
            .build();

    static final AttributeDefinition[] METRICS = { CommonAttributes.DELIVERING_COUNT, CommonAttributes.MESSAGES_ADDED,
        CommonAttributes.MESSAGE_COUNT, DURABLE_MESSAGE_COUNT, NON_DURABLE_MESSAGE_COUNT,
        SUBSCRIPTION_COUNT, DURABLE_SUBSCRIPTION_COUNT, NON_DURABLE_SUBSCRIPTION_COUNT};

    private final boolean registerRuntimeOnly;

    private final boolean deployed;

    private final List<AccessConstraintDefinition> accessConstraints;

    public static JMSTopicDefinition newDeployedJMSTopicDefinition() {
        return new JMSTopicDefinition(true, true, null, null);
    }

    public JMSTopicDefinition(final boolean registerRuntimeOnly) {
        this(registerRuntimeOnly, false, JMSTopicAdd.INSTANCE, JMSTopicRemove.INSTANCE);
    }

    private JMSTopicDefinition(final boolean registerRuntimeOnly, final boolean deployed, final OperationStepHandler addHandler, final OperationStepHandler removeHandler) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.JMS_TOPIC),
                addHandler,
                removeHandler);
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.deployed = deployed;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MessagingExtension.SUBSYSTEM_NAME, CommonAttributes.JMS_TOPIC);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        AttributeDefinition[] attributes = deployed ? getDeploymentAttributes() : ATTRIBUTES;
        for (AttributeDefinition attr : attributes) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                if (deployed) {
                    registry.registerReadOnlyAttribute(attr, JMSTopicConfigurationRuntimeHandler.INSTANCE);
                } else {
                    registry.registerReadWriteAttribute(attr, null, JMSTopicConfigurationWriteHandler.INSTANCE);
                }
            }
        }

        if (registerRuntimeOnly) {
            for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
                registry.registerReadOnlyAttribute(attr, JMSTopicReadAttributeHandler.INSTANCE);
            }

            for (AttributeDefinition metric : METRICS) {
                registry.registerMetric(metric, JMSTopicReadAttributeHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly && !deployed) {
            JMSTopicUpdateJndiHandler.registerOperations(registry, getResourceDescriptionResolver());
        }

        JMSTopicControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
