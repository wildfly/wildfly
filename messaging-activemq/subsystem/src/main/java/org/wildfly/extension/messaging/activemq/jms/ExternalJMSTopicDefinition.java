/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.External;

/**
 * Jakarta Messaging Topic resource definition
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSTopicDefinition extends PersistentResourceDefinition {

    public static final AttributeDefinition[] ATTRIBUTES = {
        CommonAttributes.DESTINATION_ENTRIES, External.ENABLE_AMQ1_PREFIX
    };

    private final boolean deployed;

    /**
     * @param deployed: indicates if this resource describe a JMS topic created via a deployment.
     */
    public ExternalJMSTopicDefinition(final boolean deployed) {
        super(MessagingExtension.EXTERNAL_JMS_TOPIC_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.EXTERNAL_JMS_TOPIC),
                ExternalJMSTopicAdd.INSTANCE,
                ExternalJMSTopicRemove.INSTANCE);
        this.deployed = deployed;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(CommonAttributes.DESTINATION_ENTRIES, External.ENABLE_AMQ1_PREFIX);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        if (deployed) {
            registry.registerReadOnlyAttribute(CommonAttributes.DESTINATION_ENTRIES, null);
            // Should this be read only as entries ?
            registry.registerReadOnlyAttribute(External.ENABLE_AMQ1_PREFIX, null);
        } else {
            registry.registerReadWriteAttribute(CommonAttributes.DESTINATION_ENTRIES, null, new ReloadRequiredWriteAttributeHandler(CommonAttributes.DESTINATION_ENTRIES));
            registry.registerReadWriteAttribute(External.ENABLE_AMQ1_PREFIX, null, new ReloadRequiredWriteAttributeHandler(External.ENABLE_AMQ1_PREFIX));
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.singletonList(MessagingExtension.JMS_TOPIC_ACCESS_CONSTRAINT);
    }
}
